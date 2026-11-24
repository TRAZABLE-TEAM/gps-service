package mx.trazable.gps.core.config.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mx.trazable.gps.core.config.websocket.records.GpsQueuedPulse;
import mx.trazable.gps.model.GpsPulseDTO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Log4j2
@RequiredArgsConstructor
public class GpsSseBroadcaster {

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutos

    // Agrupa clientes conectados por orgId
    private final Map<Integer, CopyOnWriteArrayList<SseEmitter>> clientsByOrg = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /**
     * Registra un cliente de Angular para su organización
     */
    public SseEmitter subscribe(Integer orgId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        clientsByOrg.computeIfAbsent(orgId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(orgId, emitter));
        emitter.onTimeout(() -> remove(orgId, emitter));
        emitter.onError(e -> remove(orgId, emitter));

        try {
            // Evento inicial de confirmación
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("{\"status\":\"CONNECTED\",\"orgId\":" + orgId + "}"));
        } catch (IOException e) {
            remove(orgId, emitter);
        }

        log.info("SSE CLIENT CONNECTED | orgId={} | totalActive={}", orgId, clientsByOrg.get(orgId).size());
        return emitter;
    }

    /**
     * Emite los pulsos GPS recibidos hacia los clientes Angular conectados
     */
    public void broadcast(List<GpsQueuedPulse> batch) {
        if (batch == null || batch.isEmpty()) return;

        for (GpsQueuedPulse item : batch) {
            Integer orgId = item.identity().orgId();
            List<SseEmitter> emitters = clientsByOrg.get(orgId);

            if (emitters == null || emitters.isEmpty()) {
                continue;
            }

            GpsPulseDTO pulse = item.pulse();

            try {
                String jsonPayload = objectMapper.writeValueAsString(pulse);

                for (SseEmitter emitter : emitters) {
                    try {
                        // Enviamos el pulso directamente con formato data estándar
                        emitter.send(SseEmitter.event()
                                .name("message") // nombre por defecto para que funcione directo con onmessage
                                .data(jsonPayload));
                    } catch (Exception ex) {
                        remove(orgId, emitter);
                    }
                }
            } catch (Exception e) {
                log.error("Error serializando pulso GPS para SSE: {}", e.getMessage());
            }
        }
    }

    private void remove(Integer orgId, SseEmitter emitter) {
        List<SseEmitter> emitters = clientsByOrg.get(orgId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                clientsByOrg.remove(orgId);
            }
        }
        log.debug("SSE CLIENT DISCONNECTED | orgId={}", orgId);
    }

    /**
     * Heartbeat cada 20 segundos para mantener vivas las conexiones a través de proxies y firewalls
     */
    @Scheduled(fixedRate = 20_000)
    public void keepAlive() {
        clientsByOrg.forEach((orgId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    remove(orgId, emitter);
                }
            }
        });
    }
}