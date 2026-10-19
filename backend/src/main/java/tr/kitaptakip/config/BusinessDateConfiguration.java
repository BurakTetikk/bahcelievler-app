package tr.kitaptakip.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessDateConfiguration {
    @Bean
    Clock businessClock(@Value("${app.business-time-zone:Europe/Istanbul}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
