package coffeeshout.speedtouch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@Slf4j
public class SpeedTouchGameSchedulerConfig {

    @Bean(name = "speedTouchGameScheduler")
    @Profile("!test")
    public TaskScheduler speedTouchGameScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("speed-touch-");
        scheduler.setDaemon(false);
        scheduler.setErrorHandler(t ->
                log.error("스피드 터치 스케줄 실행 중 예외가 발생했습니다.", t)
        );
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}
