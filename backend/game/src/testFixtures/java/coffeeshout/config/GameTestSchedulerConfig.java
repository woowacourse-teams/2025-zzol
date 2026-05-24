package coffeeshout.config;

import coffeeshout.game.flow.CompletableFutureFlowScheduler;
import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.support.TestTaskScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;

@Configuration
@Profile("test")
public class GameTestSchedulerConfig {

    @Bean(name = "racingGameScheduler")
    @ConditionalOnMissingBean(name = "racingGameScheduler")
    public TaskScheduler racingGameScheduler() {
        return new TestTaskScheduler();
    }

    @Bean(name = "speedTouchGameScheduler")
    @ConditionalOnMissingBean(name = "speedTouchGameScheduler")
    public TaskScheduler speedTouchGameScheduler() {
        return new TestTaskScheduler();
    }

    @Bean(name = "blindTimerGameScheduler")
    @ConditionalOnMissingBean(name = "blindTimerGameScheduler")
    public TaskScheduler blindTimerGameScheduler() {
        return new TestTaskScheduler();
    }

    @Bean(name = "cardGameFlowScheduler")
    @ConditionalOnMissingBean(name = "cardGameFlowScheduler")
    public FlowScheduler cardGameFlowScheduler() {
        return new CompletableFutureFlowScheduler(new TestTaskScheduler());
    }

    @Bean(name = "blockStackingFlowScheduler")
    @ConditionalOnMissingBean(name = "blockStackingFlowScheduler")
    public FlowScheduler blockStackingFlowScheduler() {
        return new CompletableFutureFlowScheduler(new TestTaskScheduler());
    }

    @Bean(name = "ladderFlowScheduler")
    @ConditionalOnMissingBean(name = "ladderFlowScheduler")
    public FlowScheduler ladderFlowScheduler() {
        return new CompletableFutureFlowScheduler(new TestTaskScheduler());
    }
}
