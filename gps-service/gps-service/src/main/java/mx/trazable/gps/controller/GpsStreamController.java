package mx.trazable.gps.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mx.trazable.gps.core.config.sse.GpsSseBroadcaster;
import mx.trazable.gps.security.TokenUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/gps")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Log4j2
public class GpsStreamController {

    private final GpsSseBroadcaster sseBroadcaster;
    private final TokenUtils tokenUtils;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGps(
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = tokenParam;
        if (token == null && authHeader != null) {
            token = authHeader.replace("Bearer ", "").trim();
        }

        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT requerido");
        }

        try {
            Integer orgId = tokenUtils.getOrgIdFromToken(token);
            return sseBroadcaster.subscribe(orgId);
        } catch (Exception e) {
            log.warn("Acceso SSE denegado: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
        }
    }
}