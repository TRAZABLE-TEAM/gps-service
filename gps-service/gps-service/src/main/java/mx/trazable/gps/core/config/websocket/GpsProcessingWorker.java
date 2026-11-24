package mx.trazable.gps.core.config.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mx.trazable.gps.core.config.sse.GpsSseBroadcaster;
import mx.trazable.gps.core.config.websocket.records.GpsQueuedPulse;
import mx.trazable.gps.service.GpsGeoFilterService;
import mx.trazable.gps.service.GpsPersistenceService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Procesa los pulsos GPS recibidos desde la cola.
 * <p>
 * Responsabilidades:
 * - Consumir lotes de hasta 100 pulsos.
 * - Depurar coordenadas repetidas por placa.
 * - Guardar los registros aceptados en PostgreSQL.
 * - Confirmar las posiciones después de persistir.
 * - Reintentar el lote cuando ocurre un error.
 * - Esperar durante el apagado para procesar pendientes.
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class GpsProcessingWorker {

    private static final int BATCH_SIZE = 100;
    private static final long POLL_TIMEOUT_MS = 250;
    private static final long MAX_RETRY_DELAY_MS = 30_000;
    private static final long SHUTDOWN_TIMEOUT_MS = 30_000;

    private final GpsIngestionService ingestionService;
    private final GpsPersistenceService persistenceService;
    private final GpsGeoFilterService geoFilterService;
    private final GpsSseBroadcaster sseBroadcaster;

    private volatile boolean running = true;
    private Thread worker;

    @PostConstruct
    public void start() {

        worker = new Thread(this::process, "GPS-WORKER");
        worker.start();

        log.info("GPS WORKER STARTED");
    }

    private void process() {

        List<GpsQueuedPulse> pendingBatch = null;
        int retries = 0;

        while (running
                || pendingBatch != null
                || ingestionService.getQueueSize() > 0) {

            try {

                // Obtener un nuevo lote solo si no hay uno pendiente.
                if (pendingBatch == null) {

                    GpsQueuedPulse first = ingestionService
                            .getQueue()
                            .poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                    if (first == null) {
                        continue;
                    }

                    pendingBatch = new ArrayList<>(BATCH_SIZE);
                    pendingBatch.add(first);

                    ingestionService.getQueue()
                            .drainTo(pendingBatch, BATCH_SIZE - 1);
                }

                // Filtrar por placa, distancia y tiempo.
                List<GpsQueuedPulse> filteredBatch =
                        geoFilterService.filterBatch(pendingBatch);

                // Persistir únicamente los registros aceptados.
                if (!filteredBatch.isEmpty()) {

                    persistenceService.saveBatch(filteredBatch);

                    // Actualizar caché después del guardado exitoso.
                    geoFilterService.confirmBatch(filteredBatch);

                    // NUEVO: Emitir en tiempo real al dasboard conectado via SSE
                    sseBroadcaster.broadcast(filteredBatch);
                }

                log.info(
                        "GPS FILTER | received={} | saved={} | discarded={} | queue={}",
                        pendingBatch.size(),
                        filteredBatch.size(),
                        pendingBatch.size() - filteredBatch.size(),
                        ingestionService.getQueueSize()
                );

                // El lote ya fue procesado correctamente.
                pendingBatch = null;
                retries = 0;

            } catch (InterruptedException e) {

                if (!running) {
                    break;
                }

            } catch (Exception e) {

                retries++;

                long delay = Math.min(
                        1000L * (1L << Math.min(retries - 1, 5)),
                        MAX_RETRY_DELAY_MS
                );

                log.error(
                        "GPS SAVE ERROR | attempt={} | pending={} | retryIn={}ms",
                        retries,
                        pendingBatch != null ? pendingBatch.size() : 0,
                        delay,
                        e
                );

                try {
                    Thread.sleep(delay);

                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (pendingBatch != null) {
            log.error(
                    "GPS UNSAVED RECORDS | count={}",
                    pendingBatch.size()
            );
        }

        log.info("GPS WORKER STOPPED");
    }

    @PreDestroy
    public void stop() {

        running = false;

        if (worker == null) {
            return;
        }

        try {

            worker.join(SHUTDOWN_TIMEOUT_MS);

            if (worker.isAlive()) {
                log.warn("GPS WORKER SHUTDOWN TIMEOUT");
                worker.interrupt();
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            worker.interrupt();
        }
    }
}