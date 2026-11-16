package mx.trazable.gps.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
@Entity
@Table(name = "bem_gps_tracking")
@SQLRestriction("ts_deleted IS NULL")
public class BemGpsTracking {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "bem_gps_tracking_seq"
    )
    @SequenceGenerator(
            name = "bem_gps_tracking_seq",
            sequenceName = "bem_gps_tracking_id_seq",
            allocationSize = 50
    )
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "imei")
    private String imei;

    @Column(name = "plate")
    private String plate;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "instant", nullable = false)
    private Timestamp instant;

    @Column(name = "is_ignition")
    private Boolean isIgnition;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_data", columnDefinition = "jsonb")
    private Map<String, Object> additionalData = new HashMap<>();

    @Column(name = "appointment_id", nullable = false)
    private Integer appointmentId;

    @Column(name = "ts_deleted")
    private Timestamp tsDeleted;

    @Column(name = "us_deleted")
    private Integer usDeleted;

    @Column(name = "type")
    private String type;

    @Column(name = "cat_carrier_id")
    private Integer catCarrierId;

    @Column(name = "cat_supplier_gps_id")
    private Integer catSupplierGpsId;
}