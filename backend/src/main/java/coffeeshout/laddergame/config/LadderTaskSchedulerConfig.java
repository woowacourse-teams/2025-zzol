package coffeeshout.laddergame.config;

import coffeeshout.global.flow.CompletableFutureFlowScheduler;
import coffeeshout.global.flow.FlowScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties(LadderTimingProperties.class)
public class LadderTaskSchedulerConfig {

    @Value("${ladder.scheduler.pool-size:2}")
    private int poolSize;

    @Bean(name = "ladderThreadPoolTaskScheduler")
    @Profile("!test")
    public ThreadPoolTaskScheduler ladderThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(1, poolSize));
        scheduler.setThreadNamePrefix("ladder-task-");
        scheduler.setDaemon(false);
        scheduler.setErrorHandler(t -> log.error("사다리게임 스케줄 실행 중 예외가 발생했습니다.", t));
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }

    @Bean(name = "ladderFlowScheduler")
    @Profile("!test")
    public FlowScheduler ladderFlowScheduler(ThreadPoolTaskScheduler ladderThreadPoolTaskScheduler) {
        return new CompletableFutureFlowScheduler(ladderThreadPoolTaskScheduler);
    }
}
