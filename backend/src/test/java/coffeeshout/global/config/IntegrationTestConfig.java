package coffeeshout.global.config;

import coffeeshout.cardgame.application.port.CardGameFlowScheduler;
import coffeeshout.cardgame.infra.scheduler.CompletableFutureFlowScheduler;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class IntegrationTestConfig {

    @Bean(name = "taskScheduler")
    @Primary
    public TaskScheduler noOpTaskScheduler() {
        return Mockito.mock(TaskScheduler.class, Answers.RETURNS_MOCKS);
    }

    @Bean(name = "cardGameExecutorScheduler")
    public ShutDownTestScheduler cardGameExecutorScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean
    public CardGameFlowScheduler cardGameFlowScheduler(
            ShutDownTestScheduler cardGameExecutorScheduler) {
        return new CompletableFutureFlowScheduler(cardGameExecutorScheduler);
    }

    @Bean(name = "delayRemovalScheduler")
    public TaskScheduler testIntegrationDelayRemovalScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "racingGameScheduler")
    public TaskScheduler testIntegrationRacingGameScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "speedTouchGameScheduler")
    public TaskScheduler testIntegrationSpeedTouchGameScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "blindTimerGameScheduler")
    public TaskScheduler testIntegrationBlindTimerGameScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "bombRelayGameScheduler")
    public TaskScheduler testIntegrationBombRelayGameScheduler() {
        return new ShutDownTestScheduler();
    }
}
