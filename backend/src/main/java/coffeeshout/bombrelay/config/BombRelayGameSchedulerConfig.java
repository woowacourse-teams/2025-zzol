package coffeeshout.bombrelay.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@Slf4j
public class BombRelayGameSchedulerConfig {

    @Bean(name = "bombRelayGameScheduler")
    @Profile("!test")
    public TaskScheduler bombRelayGameScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("bomb-relay-");
        scheduler.setDaemon(false);
        scheduler.setErrorHandler(t ->
                log.error("폭탄 끝말잇기 스케줄 실행 중 예외가 발생했습니다.", t)
        );
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
