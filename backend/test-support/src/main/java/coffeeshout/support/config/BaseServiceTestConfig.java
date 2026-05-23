package coffeeshout.support.config;

import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.support.ShutDownTestScheduler;
import coffeeshout.support.TestTaskScheduler;
import java.time.Clock;
import org.mockito.Answers;
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
public class BaseServiceTestConfig {

    @Bean(name = "taskScheduler")
    @Primary
    public TaskScheduler noOpTaskScheduler() {
        return Mockito.mock(TaskScheduler.class, Answers.RETURNS_MOCKS);
    }

    @Bean(name = "cardGameFlowScheduler")
    @Primary
    public FlowScheduler mockCardGameFlowScheduler() {
        return Mockito.mock(FlowScheduler.class);
    }

    @Bean(name = "blockStackingFlowScheduler")
    public FlowScheduler mockBlockStackingFlowScheduler() {
        return Mockito.mock(FlowScheduler.class);
    }

    @Bean(name = "ladderFlowScheduler")
    public FlowScheduler mockLadderFlowScheduler() {
        return Mockito.mock(FlowScheduler.class);
    }

    @Bean(name = "delayRemovalScheduler")
    public TaskScheduler testDelayRemovalScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "racingGameScheduler")
    public TaskScheduler testRacingGameScheduler() {
        return new TestTaskScheduler();
    }

    @Bean(name = "speedTouchGameScheduler")
    public TaskScheduler testSpeedTouchGameScheduler() {
        return new TestTaskScheduler();
    }

    @Bean(name = "blindTimerGameScheduler")
    public TaskScheduler testBlindTimerGameScheduler() {
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

    @Bean
    @Primary
    public Clock testClock() {
        return Clock.systemUTC();
    }
}
