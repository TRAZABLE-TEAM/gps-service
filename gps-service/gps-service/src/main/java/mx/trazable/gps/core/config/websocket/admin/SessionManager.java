package mx.trazable.gps.core.config.websocket.admin;


import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private final Map<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();

    public void register(
            String deviceKey,
            WebSocketSession session) {

        WebSocketSession previous =
                sessions.put(deviceKey, session);

        if (previous != null && previous != session) {
            try {
                if (previous.isOpen()) {
                    previous.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void remove(
            String deviceKey,
            WebSocketSession session) {

        sessions.remove(deviceKey, session);
    }

    public int getActiveConnections() {
        return sessions.size();
    }

    public boolean isCurrent(
            String deviceKey,
            WebSocketSession session) {

        return sessions.get(deviceKey) == session;
    }
}