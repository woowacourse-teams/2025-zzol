package coffeeshout.support.config;

import coffeeshout.support.ShutDownTestScheduler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class BaseIntegrationTestConfig {

    @Bean(name = "taskScheduler")
    @Primary
    public TaskScheduler noOpTaskScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "delayRemovalScheduler")
    public TaskScheduler testIntegrationDelayRemovalScheduler() {
        return new ShutDownTestScheduler();
    }
}
