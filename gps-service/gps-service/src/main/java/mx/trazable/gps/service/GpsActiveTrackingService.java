package mx.trazable.gps.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.trazable.gps.core.config.PlateUtils;
import mx.trazable.gps.model.ActiveGpsTracking;
import mx.trazable.gps.repository.GpsTrackingRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GpsActiveTrackingService {

    private final GpsTrackingRepository repository;

    private final Map<String, ActiveGpsTracking> activeTrackings =
            new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {

        for (ActiveGpsTracking tracking : repository.findActiveTrackings()) {

            if (!isValid(tracking)) {
                continue;
            }

            activeTrackings.put(
                    key(tracking.orgId(), tracking.plate()),
                    tracking
            );
        }

        log.info(
                "GPS TRACKING INITIALIZED | total={}",
                activeTrackings.size()
        );
    }

    public ActiveGpsTracking getByPlate(Integer orgId, String plate) {

        if (orgId == null || plate == null || plate.isBlank()) {
            return null;
        }

        return activeTrackings.get(key(orgId, plate));
    }

    public void add(ActiveGpsTracking tracking) {

        if (!isValid(tracking)) {
            return;
        }

        activeTrackings.put(
                key(tracking.orgId(), tracking.plate()),
                tracking
        );
    }

    public void remove(Integer orgId, String plate) {

        if (orgId == null || plate == null || plate.isBlank()) {
            return;
        }

        activeTrackings.remove(key(orgId, plate));
    }

    public int count() {
        return activeTrackings.size();
    }

    private boolean isValid(ActiveGpsTracking tracking) {

        return tracking != null
                && tracking.orgId() != null
                && tracking.plate() != null
                && !tracking.plate().isBlank()
                && tracking.lat() != null
                && tracking.lng() != null;
    }

    private String key(Integer orgId, String plate) {
        return orgId + ":" + PlateUtils.normalize(plate);
    }
}