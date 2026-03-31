package coffeeshout.numberpoker.config;

import coffeeshout.cardgame.infra.scheduler.CompletableFutureFlowScheduler;
import coffeeshout.numberpoker.application.port.NumberPokerFlowScheduler;
import coffeeshout.numberpoker.domain.NumberPokerProbabilityAdjuster;
import coffeeshout.numberpoker.infra.scheduler.CompletableFutureNumberPokerFlowScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@Configuration
@EnableConfigurationProperties({NumberPokerTimingProperties.class, NumberPokerProbabilityProperties.class})
public class NumberPokerConfig {

    @Bean(name = "numberPokerTaskScheduler")
    @Profile("!test")
    public ThreadPoolTaskScheduler numberPokerTaskScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("number-poker-");
        scheduler.setDaemon(false);
        scheduler.setErrorHandler(t -> log.error("넘버포커 스케줄 실행 중 예외가 발생했습니다.", t));
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @Profile("!test")
    public NumberPokerFlowScheduler numberPokerFlowScheduler(
            ThreadPoolTaskScheduler numberPokerTaskScheduler) {
        return new CompletableFutureNumberPokerFlowScheduler(
                new CompletableFutureFlowScheduler(numberPokerTaskScheduler));
    }

    @Bean
    public NumberPokerProbabilityAdjuster numberPokerProbabilityAdjuster(
            NumberPokerProbabilityProperties props) {
        return new NumberPokerProbabilityAdjuster(props.stage1FoldMultiplier(), props.stage2FoldMultiplier());
    }
}
