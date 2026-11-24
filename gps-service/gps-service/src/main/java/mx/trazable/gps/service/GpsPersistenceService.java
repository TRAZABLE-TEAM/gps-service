package mx.trazable.gps.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mx.trazable.gps.core.config.websocket.records.GpsQueuedPulse;
import mx.trazable.gps.core.entity.BemGpsTracking;
import mx.trazable.gps.repository.BemGpsTrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class GpsPersistenceService {

    private final BemGpsTrackingRepository repository;

    @Transactional
    public void saveBatch(List<GpsQueuedPulse> batch) {

        List<BemGpsTracking> entities = new ArrayList<>(batch.size());

        for (GpsQueuedPulse item : batch) {

            var pulse = item.pulse();
            var identity = item.identity();

            BemGpsTracking entity = new BemGpsTracking();

            entity.setImei(pulse.imei());
            entity.setPlate(pulse.plate());
            entity.setLatitude(pulse.latitude());
            entity.setLongitude(pulse.longitude());

            entity.setInstant(Timestamp.valueOf(LocalDateTime.ofInstant(pulse.instant(), ZoneOffset.UTC)));

            entity.setIsIgnition(pulse.isIgnition());
            entity.setType(pulse.type());
            entity.setAppointmentId(pulse.appointmentId());

            entity.setAdditionalData(pulse.additionalData() != null ? pulse.additionalData() : Map.of());

            entity.setCatCarrierId(identity.carrierId());
            entity.setCatSupplierGpsId(identity.supplierGpsId());

            entities.add(entity);
        }

        repository.saveAllAndFlush(entities);

        log.info("GPS SAVED | records={}", entities.size());
    }
}