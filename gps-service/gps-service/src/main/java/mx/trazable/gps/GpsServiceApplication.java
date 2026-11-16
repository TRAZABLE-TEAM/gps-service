package mx.trazable.gps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication(
        exclude = {
                org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class
        }
)public class GpsServiceApplication {

	public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
        SpringApplication.run(GpsServiceApplication.class, args);
	}

}
