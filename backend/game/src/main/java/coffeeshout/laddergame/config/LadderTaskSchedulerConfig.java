package coffeeshout.laddergame.config;

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
@EnableConfigurationProperties(LadderTimingProperties.class)
public class LadderTaskSchedulerConfig {

    @Value("${ladder.scheduler.pool-size:2}")
    private int poolSize;

    @Bean(name = "ladderThreadPoolTaskScheduler")
    @Profile("!test")
    public ThreadPoolTaskScheduler ladderThreadPoolTaskScheduler(GameTaskSchedulerFactory schedulerFactory) {
        return schedulerFactory.create("ladder-task-", poolSize, "사다리게임 스케줄 실행 중 예외가 발생했습니다.");
    }

    @Bean(name = "ladderFlowScheduler")
    @Profile("!test")
    public FlowScheduler ladderFlowScheduler(ThreadPoolTaskScheduler ladderThreadPoolTaskScheduler) {
        return new CompletableFutureFlowScheduler(ladderThreadPoolTaskScheduler);
    }
}
