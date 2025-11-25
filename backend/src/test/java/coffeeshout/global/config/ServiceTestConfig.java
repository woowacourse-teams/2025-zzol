package coffeeshout.global.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class ServiceTestConfig {

    @Bean(name = "cardGameTaskScheduler")
    public TaskScheduler testCardGameTaskScheduler() {
        return new TestTaskScheduler();
    }

    @Bean(name = "delayRemovalScheduler")
    public TaskScheduler testDelayRemovalScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "racingGameScheduler")
    public TaskScheduler testRacingGameScheduler() {
        return new TestTaskScheduler();
    }

    @Bean
    @Primary
    public SimpMessagingTemplate mockMessagingTemplate() {
        return Mockito.mock(SimpMessagingTemplate.class);
    }

    @Bean
    @Primary
    public ApplicationEventPublisher mockEventPublisher() {
        return Mockito.mock(ApplicationEventPublisher.class);
    }
}
