package mx.trazable.gps.service;


import lombok.AllArgsConstructor;
import mx.trazable.gps.core.config.WebServiceResponse;
import mx.trazable.gps.core.exception.ResourceNotFoundException;
import mx.trazable.gps.model.GenerateJwtForSupplierDTO;
import mx.trazable.gps.repository.ValidationRepository;
import mx.trazable.gps.security.TokenUtils;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ConfigJwtService {

    private final TokenUtils tokenUtils;
    private final ValidationRepository validationsRepository;


    public WebServiceResponse generateJwtForSupplierGps(GenerateJwtForSupplierDTO dto) {

        validJwt(dto.getOrgId(), dto.getSupplierGpsId(), dto.getCarrierId());

        return new WebServiceResponse(true, "Se genero correctamente el jwt", tokenUtils.createToken(dto.getSupplierGpsId(), dto.getCarrierId(), dto.getOrgId()));
    }

    public void validJwt(Integer orgId, Integer supplierGpsId, Integer carrierId) {
        if (!validationsRepository.existOrg(orgId))
            throw new ResourceNotFoundException("No se encontró la organization: " + orgId);

        if (!validationsRepository.existSupplierGps(supplierGpsId, orgId))
            throw new ResourceNotFoundException("No se encontró el proveedor de GPS: " + supplierGpsId);

        if (!validationsRepository.existCarrier(carrierId, orgId))
            throw new ResourceNotFoundException("No se encontró la linea transportista: " + carrierId);
    }
}
