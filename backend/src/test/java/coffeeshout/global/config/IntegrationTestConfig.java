package coffeeshout.global.config;

import coffeeshout.cardgame.application.port.CardGameFlowScheduler;
import coffeeshout.cardgame.infra.scheduler.CompletableFutureFlowScheduler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;

@TestConfiguration
@Profile("test")
@Import(TestContainerConfig.class)
public class IntegrationTestConfig {

    @Bean
    public CardGameFlowScheduler cardGameFlowScheduler() {
        ShutDownTestScheduler scheduler = new ShutDownTestScheduler();
        return new CompletableFutureFlowScheduler(scheduler.getScheduledExecutor());
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
