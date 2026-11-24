package mx.trazable.gps.core.config.websocket.admin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mx.trazable.gps.core.config.PlateUtils;
import mx.trazable.gps.core.config.websocket.GpsIngestionService;
import mx.trazable.gps.core.config.websocket.records.GpsIdentity;
import mx.trazable.gps.model.ActiveGpsTracking;
import mx.trazable.gps.model.GpsPulseDTO;
import mx.trazable.gps.service.GpsActiveTrackingService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Gestiona la recepción de pulsos GPS mediante WebSocket.
 * <p>
 * Responsabilidades:
 * - Validar la identidad de la conexión.
 * - Convertir y validar los mensajes JSON.
 * - Limitar la recepción a un pulso cada 3 segundos por placa.
 * - Enviar los pulsos aceptados a la cola de procesamiento.
 * - Administrar la conexión y desconexión de los clientes.
 * <p>
 * El control de frecuencia utiliza una caché con expiración
 * para evitar crecimiento indefinido de memoria.
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class GpsWebSocketHandler extends TextWebSocketHandler {

    private static final long MIN_INTERVAL_MS = 3_000;

    private final SessionManager sessionManager;
    private final GpsIngestionService ingestionService;
    private final ObjectMapper objectMapper;
    private final GpsActiveTrackingService gpsActiveTrackingService;

    /**
     * Almacena el último instante de recepción aceptado
     * por organización y placa.
     * <p>
     * Las entradas inactivas expiran después de 30 minutos.
     */
    private final Cache<String, Long> lastReceived = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session) throws Exception {

        GpsIdentity identity = identity(session);

        if (identity == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        session.setTextMessageSizeLimit(65536);

        sessionManager.register(session.getId(), session);

        log.info(
                "GPS CONNECTED | session={} | supplier={} | carrier={} | active={}",
                session.getId(),
                identity.supplierGpsId(),
                identity.carrierId(),
                sessionManager.getActiveConnections()
        );
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        GpsIdentity identity = identity(session);

        if (identity == null ||
                !sessionManager.isCurrent(session.getId(), session)) {

            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        GpsPulseDTO pulse;

        try {
            pulse = objectMapper.readValue(message.getPayload(), GpsPulseDTO.class);
        } catch (Exception e) {
            log.warn("GPS INVALID JSON | session={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        if (!isValid(pulse)) {
            log.warn(
                    "GPS INVALID PULSE | session={} | payload={}",
                    session.getId(),
                    message.getPayload()
            );
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String plate = PlateUtils.normalize(pulse.plate());

        ActiveGpsTracking tracking = gpsActiveTrackingService.getByPlate(
                identity.orgId(),
                plate
        );

        if (tracking == null) {
            log.debug("GPS INACTIVE PLATE | plate={} | org={}",
                    plate, identity.orgId());
            return;
        }
        // Verificar que el viaje corresponda al transportista.
//        if (tracking.carrierId() == null ||
//                !tracking.carrierId().equals(identity.carrierId())) {
//
//            log.warn("GPS CARRIER MISMATCH | plate={} | carrier={}", plate, identity.carrierId());
//            return;
//        }

        String key = identity.orgId() + ":" + plate;
        long now = System.currentTimeMillis();

        final boolean[] allowed = {false};

        lastReceived.asMap().compute(key, (k, previous) -> {

            if (previous == null || now - previous >= MIN_INTERVAL_MS) {

                allowed[0] = true;
                return now;
            }

            return previous;
        });

        if (!allowed[0]) {
            log.debug(
                    "GPS RATE LIMIT | plate={} | org={}",
                    plate,
                    identity.orgId()
            );
            return;
        }

        // El appointmentId se obtiene del viaje activo.
        GpsPulseDTO enrichedPulse = new GpsPulseDTO(
                pulse.messageId(),
                pulse.imei(),
                pulse.plate(),
                pulse.latitude(),
                pulse.longitude(),
                pulse.instant(),
                pulse.isIgnition(),
                pulse.type(),
                tracking.appointmentId(),
                pulse.additionalData()
        );

        boolean accepted = ingestionService.enqueue(identity, enrichedPulse);

        if (!accepted) {

            lastReceived.asMap().remove(key, now);

            log.warn("GPS QUEUE FULL | plate={} | org={}", plate, identity.orgId());

            session.close(new CloseStatus(1013, "Server overloaded"));
            return;
        }

        log.debug(
                "GPS RECEIVED | plate={} | appointment={} | org={} | queueSize={}",
                plate,
                tracking.appointmentId(),
                identity.orgId(),
                ingestionService.getQueueSize()
        );
    }

    /**
     * Valida los campos mínimos necesarios para procesar
     * y almacenar una ubicación GPS.
     */
    private boolean isValid(GpsPulseDTO pulse) {

        if (pulse == null ||
                pulse.plate() == null ||
                pulse.plate().isBlank() ||
                pulse.instant() == null ||
                pulse.latitude() == null ||
                pulse.longitude() == null) {

            return false;
        }

        return pulse.latitude().compareTo(BigDecimal.valueOf(-90)) >= 0
                && pulse.latitude().compareTo(BigDecimal.valueOf(90)) <= 0
                && pulse.longitude().compareTo(BigDecimal.valueOf(-180)) >= 0
                && pulse.longitude().compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    /**
     * Recupera la identidad validada durante el handshake.
     */
    private GpsIdentity identity(WebSocketSession session) {

        return (GpsIdentity) session.getAttributes()
                .get("gpsIdentity");
    }

    /**
     * Libera la sesión WebSocket.
     * <p>
     * No elimina el control de frecuencia de las placas,
     * ya que podría haber otras conexiones enviando datos
     * de los mismos vehículos.
     */
    private void cleanup(WebSocketSession session) {

        sessionManager.remove(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        cleanup(session);

        log.info(
                "GPS DISCONNECTED | session={} | status={} | active={}",
                session.getId(),
                status,
                sessionManager.getActiveConnections()
        );
    }

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception) throws Exception {

        log.warn(
                "GPS TRANSPORT ERROR | session={} | error={}",
                session.getId(),
                exception.getMessage()
        );

        cleanup(session);

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}