package mx.trazable.gps.core.config.websocket.records;

public record GpsIdentity(
        Integer orgId,
        Integer supplierGpsId,
        Integer carrierId
) {}