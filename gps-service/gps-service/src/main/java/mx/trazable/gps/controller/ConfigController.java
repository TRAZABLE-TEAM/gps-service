package mx.trazable.gps.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mx.trazable.gps.core.config.WebServiceResponse;
import mx.trazable.gps.model.GenerateJwtForSupplierDTO;
import mx.trazable.gps.service.ConfigJwtService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Log4j2
@CrossOrigin(origins = "*")
@RequestMapping("/config")
public class ConfigController {

    private final ConfigJwtService configJwtService;

    @PostMapping()
    public WebServiceResponse getJwtForSupplier(@Valid @RequestBody GenerateJwtForSupplierDTO dto) {
        return configJwtService.generateJwtForSupplierGps(dto);
    }
}
