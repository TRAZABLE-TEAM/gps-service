package mx.trazable.gps.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import mx.trazable.gps.model.ActiveGpsTracking;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GpsTrackingRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<ActiveGpsTracking> findActiveTrackings() {

        String sql = """
                SELECT DISTINCT
                    t.appointment_id,
                    a.org_id,
                    truck.plate,
                    carrier.id AS carrier_id,

                    CASE
                        WHEN a.counterparty ->> 'targetRecord' = 'PLANTA'
                            THEN (plant.location ->> 'lat')::double precision
                        WHEN a.counterparty ->> 'targetRecord' = 'CLIENTE'
                            THEN NULL::double precision
                        ELSE NULL::double precision
                    END AS lat,

                    CASE
                        WHEN a.counterparty ->> 'targetRecord' = 'PLANTA'
                            THEN (plant.location ->> 'lng')::double precision
                        WHEN a.counterparty ->> 'targetRecord' = 'CLIENTE'
                            THEN NULL::double precision
                        ELSE NULL::double precision
                    END AS lng,

                    t.status

                FROM bem_appointment_tracking t

                INNER JOIN bem_appointment_unit_validations vali
                    ON vali.appointment_id = t.appointment_id

                INNER JOIN bem_appointment a
                    ON a.id = t.appointment_id
                    AND a.ts_deleted IS NULL

                LEFT JOIN bem_cat_truck truck
                    ON (vali.carrier_data ->> 'catTruckId')::int = truck.id
                    AND truck.ts_deleted IS NULL

                LEFT JOIN bem_cat_carrier carrier
                    ON truck.cat_carrier_id = carrier.id
                    AND carrier.ts_deleted IS NULL

                LEFT JOIN bem_cat_plant plant
                    ON a.counterparty ->> 'targetRecord' = 'PLANTA'
                    AND (a.counterparty ->> 'targetId')::int = plant.id
                    AND plant.ts_deleted IS NULL

                LEFT JOIN bem_cat_client client
                    ON a.counterparty ->> 'targetRecord' = 'CLIENTE'
                    AND (a.counterparty ->> 'targetId')::int = client.id
                    AND client.ts_deleted IS NULL

                WHERE t.ts_deleted IS NULL
                  AND vali.ts_deleted IS NULL
                  AND vali.is_contracted IS TRUE
                  AND t.status = 'ACTIVE'
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager
                .createNativeQuery(sql)
                .getResultList();

        return results.stream()
                .map(row -> new ActiveGpsTracking(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        (String) row[2],
                        row[3] == null ? null : ((Number) row[3]).intValue(),
                        row[4] == null ? null : ((Number) row[4]).doubleValue(),
                        row[5] == null ? null : ((Number) row[5]).doubleValue(),
                        100.0,
                        (String) row[6]
                ))
                .toList();
    }
}