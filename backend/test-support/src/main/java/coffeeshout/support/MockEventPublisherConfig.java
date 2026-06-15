package coffeeshout.support;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class MockEventPublisherConfig {

    @Bean
    @Primary
    public ApplicationEventPublisher mockEventPublisher() {
        return Mockito.mock(ApplicationEventPublisher.class);
    }
}
