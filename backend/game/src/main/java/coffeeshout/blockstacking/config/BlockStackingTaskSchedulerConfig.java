package coffeeshout.blockstacking.config;

import coffeeshout.game.flow.CompletableFutureFlowScheduler;
import coffeeshout.game.scheduler.GameTaskSchedulerFactory;
import coffeeshout.gamecommon.flow.FlowScheduler;
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
public class BlockStackingTaskSchedulerConfig {

    @Value("${block-stacking.scheduler.pool-size:2}")
    private int poolSize;

    @Bean(name = "blockStackingThreadPoolTaskScheduler")
    @Profile("!test")
    public ThreadPoolTaskScheduler blockStackingThreadPoolTaskScheduler(GameTaskSchedulerFactory schedulerFactory) {
        return schedulerFactory.create("block-stacking-task-", poolSize, "블록 쌓기 스케줄 실행 중 예외가 발생했습니다.");
    }

    @Bean(name = "blockStackingFlowScheduler")
    @Profile("!test")
    public FlowScheduler blockStackingFlowScheduler(
            ThreadPoolTaskScheduler blockStackingThreadPoolTaskScheduler) {
        return new CompletableFutureFlowScheduler(blockStackingThreadPoolTaskScheduler);
    }
}
