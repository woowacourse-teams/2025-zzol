package coffeeshout.global.config;

import coffeeshout.support.ShutDownTestScheduler;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class CommonTestSchedulerConfig {

    @Bean(name = "taskScheduler")
    @Primary
    public TaskScheduler noOpTaskScheduler() {
        return Mockito.mock(TaskScheduler.class, Answers.RETURNS_MOCKS);
    }

    @Bean(name = "delayRemovalScheduler")
    public TaskScheduler testDelayRemovalScheduler() {
        return new ShutDownTestScheduler();
    }
}
