package mx.trazable.gps.core.config.websocket.records;


import mx.trazable.gps.model.GpsPulseDTO;

public record GpsQueuedPulse(
        GpsIdentity identity,
        GpsPulseDTO pulse
) {}