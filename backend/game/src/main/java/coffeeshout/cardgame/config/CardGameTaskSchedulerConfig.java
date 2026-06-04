package coffeeshout.cardgame.config;

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
@EnableConfigurationProperties(CardGameTimingProperties.class)
public class CardGameTaskSchedulerConfig {

    @Bean(name = "cardGameThreadPoolTaskScheduler")
    @Profile("!test")
    public ThreadPoolTaskScheduler cardGameThreadPoolTaskScheduler(GameTaskSchedulerFactory schedulerFactory) {
        return schedulerFactory.create("card-game-task-", 3, "스케줄 실행 중 예외가 발생했습니다.");
    }

    @Bean
    @Profile("!test")
    public FlowScheduler cardGameFlowScheduler(ThreadPoolTaskScheduler cardGameThreadPoolTaskScheduler) {
        return new CompletableFutureFlowScheduler(cardGameThreadPoolTaskScheduler);
    }
}
