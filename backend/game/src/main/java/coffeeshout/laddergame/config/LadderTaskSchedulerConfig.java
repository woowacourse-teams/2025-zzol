package coffeeshout.laddergame.config;

import coffeeshout.game.flow.CompletableFutureFlowScheduler;
import coffeeshout.game.scheduler.GameTaskSchedulerFactory;
import coffeeshout.gamecommon.flow.FlowScheduler;
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

    @Bean(name = "ladderThreadPoolTaskScheduler")
    @Profile("!test")
    public ThreadPoolTaskScheduler ladderThreadPoolTaskScheduler(GameTaskSchedulerFactory schedulerFactory) {
        return schedulerFactory.create("ladder");
    }

    @Bean(name = "ladderFlowScheduler")
    @Profile("!test")
    public FlowScheduler ladderFlowScheduler(ThreadPoolTaskScheduler ladderThreadPoolTaskScheduler) {
        return new CompletableFutureFlowScheduler(ladderThreadPoolTaskScheduler);
    }
}
