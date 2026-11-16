package mx.trazable.gps.core.config.websocket;

import lombok.RequiredArgsConstructor;
import mx.trazable.gps.core.config.websocket.admin.GpsWebSocketHandler;
import mx.trazable.gps.core.config.websocket.validationsJwt.GpsHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final GpsWebSocketHandler gpsWebSocketHandler;
    private final GpsHandshakeInterceptor gpsHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry) {

        registry.addHandler(gpsWebSocketHandler, "/ws/gps").addInterceptors(gpsHandshakeInterceptor).setAllowedOriginPatterns("*");
    }
}