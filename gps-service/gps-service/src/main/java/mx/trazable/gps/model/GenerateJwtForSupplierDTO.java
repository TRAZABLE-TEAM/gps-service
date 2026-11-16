package mx.trazable.gps.model;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateJwtForSupplierDTO {

    @NotNull(message = "supplierGpsId es obligatorio")
    Integer supplierGpsId;

    @NotNull(message = "carrierId es obligatorio")
    Integer carrierId;

    @NotNull(message = "orgId es obligatorio")
    Integer orgId;
}
