package mx.trazable.gps.core.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "properties.core")
public class CoreConfig {

    private AccessToken accessToken = new AccessToken();

    @Data
    public static class AccessToken {
        private String publicKey;
        private String privateKey;
    }
}
