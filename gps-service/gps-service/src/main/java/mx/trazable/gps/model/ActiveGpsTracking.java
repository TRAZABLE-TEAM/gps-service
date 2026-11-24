package mx.trazable.gps.model;

import java.util.Map;

public record ActiveGpsTracking(
        Integer appointmentId,
        Integer orgId,
        String plate,
        Integer carrierId,
        Double lat,
        Double lng,
        Double radius,
        String status
) {}