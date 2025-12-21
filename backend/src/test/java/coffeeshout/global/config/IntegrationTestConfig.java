package coffeeshout.global.config;

import coffeeshout.global.redis.stream.StreamPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestConfiguration
@Profile("test")
@Import(TestContainerConfig.class)
public class IntegrationTestConfig {

    @MockitoBean
    private StreamPublisher streamPublisher;

    @Bean(name = "cardGameTaskScheduler")
    public TaskScheduler testIntegrationCardGameTaskScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "delayRemovalScheduler")
    public TaskScheduler testIntegrationDelayRemovalScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "racingGameScheduler")
    public TaskScheduler testIntegrationRacingGameScheduler() {
        return new ShutDownTestScheduler();
    }
}
