package coffeeshout.support.config;

import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.support.ShutDownTestScheduler;
import coffeeshout.support.TestTaskScheduler;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;

// 비-game 모듈 통합 테스트용: FlowScheduler는 mock, 나머지 스케줄러는 실제 구현
@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class BaseIntegrationTestConfig {

    @Bean(name = "taskScheduler")
    @Primary
    public TaskScheduler noOpTaskScheduler() {
        return new ShutDownTestScheduler();
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
    public TaskScheduler testIntegrationDelayRemovalScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "racingGameScheduler")
    public TaskScheduler testIntegrationRacingGameScheduler() {
        return new TestTaskScheduler();
    }

    @Bean(name = "speedTouchGameScheduler")
    public TaskScheduler testIntegrationSpeedTouchGameScheduler() {
        return new TestTaskScheduler();
    }

    @Bean(name = "blindTimerGameScheduler")
    public TaskScheduler testIntegrationBlindTimerGameScheduler() {
        return new TestTaskScheduler();
    }
}
