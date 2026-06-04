package coffeeshout.blindtimer.config;

import io.micrometer.context.ContextSnapshotFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@Slf4j
public class BlindTimerGameSchedulerConfig {

    @Bean(name = "blindTimerGameScheduler")
    @Profile("!test")
    public TaskScheduler blindTimerGameScheduler(ContextSnapshotFactory snapshotFactory) {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("blind-timer-");
        scheduler.setDaemon(false);
        // 지연 실행 후 Stream 발행 시 trace가 끊기지 않도록 제출 시점 컨텍스트를 전파한다
        scheduler.setTaskDecorator(runnable -> snapshotFactory.captureAll().wrap(runnable));
        scheduler.setErrorHandler(t ->
                log.error("블라인드 타이머 스케줄 실행 중 예외가 발생했습니다.", t)
        );
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
