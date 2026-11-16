package mx.trazable.gps.model;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record GpsPulseDTO(
        String messageId,
        String imei,
        String plate,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant instant,
        Boolean isIgnition,
        String type,
        Integer appointmentId,
        Map<String, Object> additionalData
) {}