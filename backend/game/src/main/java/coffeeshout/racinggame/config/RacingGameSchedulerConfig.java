package coffeeshout.racinggame.config;

import coffeeshout.racinggame.domain.TapPerSecondSpeedCalculator;
import io.micrometer.context.ContextSnapshotFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@Slf4j
public class RacingGameSchedulerConfig {

    @Bean
    public TapPerSecondSpeedCalculator tapPerSecondSpeedCalculator() {
        return new TapPerSecondSpeedCalculator();
    }

    @Bean(name = "racingGameScheduler")
    @Profile("!test")
    public TaskScheduler racingGameScheduler(ContextSnapshotFactory snapshotFactory) {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("racing-game-");
        scheduler.setDaemon(false);
        // 지연 실행 후 Stream 발행 시 trace가 끊기지 않도록 제출 시점 컨텍스트를 전파한다
        scheduler.setTaskDecorator(runnable -> snapshotFactory.captureAll().wrap(runnable));
        scheduler.setErrorHandler(t ->
                log.error("스케줄 실행 중 예외가 발생했습니다.", t)
        );
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}
