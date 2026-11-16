package mx.trazable.gps.core.config.websocket.validationsJwt;


import lombok.RequiredArgsConstructor;
import mx.trazable.gps.core.config.websocket.records.GpsIdentity;
import mx.trazable.gps.security.TokenUtils;
import mx.trazable.gps.service.ConfigJwtService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GpsTokenValidator {

    private final TokenUtils tokenUtils;
    private final ConfigJwtService configJwtService;

    public GpsIdentity validate(String token) {

        // Estos métodos deben existir en TokenUtils
        // y validar la firma y expiración del JWT.
        Integer orgId = tokenUtils.getOrgIdFromToken(token);
        Integer supplierGpsId = tokenUtils.getSupplierGpsIdFromToken(token);
        Integer carrierId = tokenUtils.getCarrierIdFromToken(token);

        if (orgId == null || supplierGpsId == null || carrierId == null) {
            throw new SecurityException("JWT GPS inválido");
        }

        configJwtService.validJwt(orgId, supplierGpsId, carrierId);
        // También validar el claim scope = gps:write
        // y que proveedor/transportista estén autorizados.

        return new GpsIdentity(orgId, supplierGpsId, carrierId);
    }
}