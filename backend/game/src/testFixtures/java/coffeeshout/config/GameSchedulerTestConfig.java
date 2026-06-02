package coffeeshout.config;

import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.support.TestTaskScheduler;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;

@TestConfiguration(proxyBeanMethods = false)
public class GameSchedulerTestConfig {

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
}
