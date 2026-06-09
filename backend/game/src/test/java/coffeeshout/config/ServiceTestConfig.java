package coffeeshout.config;

import java.time.Clock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
@Import({GameSchedulerTestConfig.class, ExternalPortMockConfig.class, MiniGameFactoryTestConfig.class})
public class ServiceTestConfig {

    @Bean
    @Primary
    public SimpMessagingTemplate mockMessagingTemplate() {
        return Mockito.mock(SimpMessagingTemplate.class);
    }

    @Bean
    @Primary
    public Clock testClock() {
        return Clock.systemUTC();
    }
}
