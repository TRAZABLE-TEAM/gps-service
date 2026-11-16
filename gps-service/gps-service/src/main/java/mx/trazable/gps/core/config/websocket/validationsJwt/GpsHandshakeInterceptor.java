package mx.trazable.gps.core.config.websocket.validationsJwt;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mx.trazable.gps.core.config.websocket.records.GpsIdentity;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class GpsHandshakeInterceptor implements HandshakeInterceptor {

    private final GpsTokenValidator tokenValidator;
    private static final long MIN_INTERVAL_MS = 3_000;
    private final ConcurrentHashMap<String, Long> lastReceived = new ConcurrentHashMap<>();

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Map<String, Object> attributes) {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        HttpServletRequest httpRequest =
                servletRequest.getServletRequest();

        String authorization =
                httpRequest.getHeader("Authorization");

        if (authorization == null) {

            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String token = authorization.trim();

            GpsIdentity identity = tokenValidator.validate(token);

            if (identity == null ||
                    identity.orgId() == null ||
                    identity.supplierGpsId() == null ||
                    identity.carrierId() == null) {

                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put("gpsIdentity", identity);

            return true;

        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Exception exception) {
    }
}