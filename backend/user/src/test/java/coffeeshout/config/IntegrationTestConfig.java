package coffeeshout.config;

import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class IntegrationTestConfig {

    @Bean
    @Primary
    public Clock testClock() {
        return Clock.systemUTC();
    }
}
