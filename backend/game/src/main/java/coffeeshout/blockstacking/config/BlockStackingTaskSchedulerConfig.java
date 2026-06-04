package coffeeshout.blockstacking.config;

import coffeeshout.game.flow.CompletableFutureFlowScheduler;
import coffeeshout.gamecommon.flow.FlowScheduler;
import io.micrometer.context.ContextSnapshotFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${block-stacking.scheduler.pool-size:2}")
    private int poolSize;

    @Bean(name = "blockStackingThreadPoolTaskScheduler")
    @Profile("!test")
    public ThreadPoolTaskScheduler blockStackingThreadPoolTaskScheduler(ContextSnapshotFactory snapshotFactory) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(1, poolSize));
        scheduler.setThreadNamePrefix("block-stacking-task-");
        scheduler.setDaemon(false);
        // 지연 실행 후 Stream 발행 시 trace가 끊기지 않도록 제출 시점 컨텍스트를 전파한다
        scheduler.setTaskDecorator(runnable -> snapshotFactory.captureAll().wrap(runnable));
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
