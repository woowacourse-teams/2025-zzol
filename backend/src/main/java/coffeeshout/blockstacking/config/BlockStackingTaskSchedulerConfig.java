package coffeeshout.blockstacking.config;

import coffeeshout.global.flow.CompletableFutureFlowScheduler;
import coffeeshout.global.flow.FlowScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BlockStackingTimingProperties.class)
@Slf4j
public class BlockStackingTaskSchedulerConfig {

    @Bean(name = "blockStackingThreadPoolTaskScheduler")
    @Profile("!test")
    public ThreadPoolTaskScheduler blockStackingThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("block-stacking-task-");
        scheduler.setDaemon(false);
        scheduler.setErrorHandler(t -> log.error("블록 쌓기 스케줄 실행 중 예외가 발생했습니다.", t));
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }

    @Bean(name = "blockStackingFlowScheduler")
    @Profile("!test")
    public FlowScheduler blockStackingFlowScheduler(
            ThreadPoolTaskScheduler blockStackingThreadPoolTaskScheduler) {
        return new CompletableFutureFlowScheduler(blockStackingThreadPoolTaskScheduler);
    }
}
