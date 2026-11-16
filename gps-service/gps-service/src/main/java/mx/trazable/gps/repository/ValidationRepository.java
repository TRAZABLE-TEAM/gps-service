package mx.trazable.gps.repository;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class ValidationRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existOrg(Integer pOrgId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM core_org
                    WHERE id = ?
                      AND ts_deleted IS NULL
                )
                """, Boolean.class, pOrgId));
    }

    public boolean existSupplierGps(Integer pSupplierId, Integer pOrgId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM bem_cat_supplier_gps
                    WHERE id = ?
                      AND org_id = ?
                      AND ts_deleted IS NULL
                )
                """, Boolean.class, pSupplierId, pOrgId));
    }

    public boolean existCarrier(Integer pCarrierId, Integer pOrgId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM bem_cat_carrier
                    WHERE id = ?
                      AND org_id = ?
                      AND ts_deleted IS NULL
                )
                """, Boolean.class, pCarrierId, pOrgId));
    }
}