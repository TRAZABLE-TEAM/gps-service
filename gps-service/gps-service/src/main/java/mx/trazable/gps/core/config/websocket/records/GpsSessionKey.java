package mx.trazable.gps.core.config.websocket.records;

public record GpsSessionKey(
        Integer orgId,
        Integer supplierGpsId,
        Integer carrierId,
        String imei
) {}