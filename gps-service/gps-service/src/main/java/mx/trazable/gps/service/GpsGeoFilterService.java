package mx.trazable.gps.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import mx.trazable.gps.core.config.websocket.records.GpsQueuedPulse;
import mx.trazable.gps.model.GpsPulseDTO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Filtra las coordenadas GPS por organización y placa.
 *
 * Evita guardar ubicaciones repetidas o muy cercanas.
 * Conserva eventos importantes y actualiza la última
 * ubicación únicamente después de persistir en PostgreSQL.
 *
 * Utiliza Caffeine para limitar el consumo de memoria.
 */
@Service
public class GpsGeoFilterService {

    private static final double MIN_DISTANCE_METERS = 15.0;
    private static final long MAX_INTERVAL_SECONDS = 60;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final Cache<String, LastGpsPosition> lastAccepted =
            Caffeine.newBuilder()
                    .maximumSize(10_000)
                    .expireAfterAccess(30, TimeUnit.MINUTES)
                    .build();

    /**
     * Evalúa los pulsos de un lote sin modificar
     * las posiciones confirmadas en memoria.
     */
    public List<GpsQueuedPulse> filterBatch(List<GpsQueuedPulse> batch) {

        Map<String, LastGpsPosition> pending = new HashMap<>();
        List<GpsQueuedPulse> accepted = new ArrayList<>();

        for (GpsQueuedPulse item : batch) {

            String key = key(item);

            LastGpsPosition previous = pending.containsKey(key)
                    ? pending.get(key)
                    : lastAccepted.getIfPresent(key);

            GpsPulseDTO current = item.pulse();

            if (!shouldSave(current, previous)) {
                continue;
            }

            accepted.add(item);

            if (previous == null ||
                    current.instant().isAfter(previous.instant())) {

                pending.put(key, toPosition(current));
            }
        }

        return accepted;
    }

    /**
     * Confirma las posiciones únicamente después
     * de guardar correctamente el lote en PostgreSQL.
     */
    public void confirmBatch(List<GpsQueuedPulse> savedBatch) {

        for (GpsQueuedPulse item : savedBatch) {

            String key = key(item);
            LastGpsPosition current = toPosition(item.pulse());

            lastAccepted.asMap().compute(key, (k, previous) -> {

                if (previous == null ||
                        current.instant().isAfter(previous.instant())) {
                    return current;
                }

                return previous;
            });
        }
    }

    /**
     * Determina si una coordenada merece almacenarse.
     */
    private boolean shouldSave(
            GpsPulseDTO current,
            LastGpsPosition previous) {

        // Conservar todos los eventos especiales.
        if (!"LOCATION".equalsIgnoreCase(current.type())) {
            return true;
        }

        // Primera ubicación de la placa.
        if (previous == null) {
            return true;
        }

        // Descartar ubicaciones antiguas o repetidas.
        if (!current.instant().isAfter(previous.instant())) {
            return false;
        }

        // Conservar cambios importantes.
        if (!Objects.equals(
                current.appointmentId(),
                previous.appointmentId()
        ) || !Objects.equals(
                current.isIgnition(),
                previous.isIgnition()
        )) {
            return true;
        }

        long elapsed = Duration.between(
                previous.instant(),
                current.instant()
        ).getSeconds();

        double distance = calculateDistance(
                previous.latitude(),
                previous.longitude(),
                current.latitude().doubleValue(),
                current.longitude().doubleValue()
        );

        return distance >= MIN_DISTANCE_METERS
                || elapsed >= MAX_INTERVAL_SECONDS;
    }

    /**
     * Genera una clave única por organización y placa.
     */
    private String key(GpsQueuedPulse item) {

        String plate = item.pulse()
                .plate()
                .trim()
                .toUpperCase(Locale.ROOT);

        return item.identity().orgId() + ":" + plate;
    }

    /**
     * Convierte un pulso a una representación ligera
     * para reducir el consumo de memoria.
     */
    private LastGpsPosition toPosition(GpsPulseDTO pulse) {

        return new LastGpsPosition(
                pulse.latitude().doubleValue(),
                pulse.longitude().doubleValue(),
                pulse.instant(),
                pulse.isIgnition(),
                pulse.appointmentId()
        );
    }

    /**
     * Calcula la distancia entre coordenadas en metros
     * utilizando la fórmula de Haversine.
     */
    private double calculateDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double latDiff = Math.toRadians(lat2 - lat1);
        double lonDiff = Math.toRadians(lon2 - lon1);

        double a = Math.pow(Math.sin(latDiff / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(lonDiff / 2), 2);

        a = Math.max(0.0, Math.min(1.0, a));

        return EARTH_RADIUS_METERS * 2
                * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Representa únicamente los datos necesarios
     * para comparar la última ubicación de una placa.
     */
    private record LastGpsPosition(
            double latitude,
            double longitude,
            Instant instant,
            Boolean isIgnition,
            Integer appointmentId
    ) {
    }
}