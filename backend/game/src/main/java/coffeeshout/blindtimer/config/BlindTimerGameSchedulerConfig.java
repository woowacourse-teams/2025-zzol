package coffeeshout.blindtimer.config;

import coffeeshout.game.scheduler.GameTaskSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;

@Configuration
public class BlindTimerGameSchedulerConfig {

    @Bean(name = "blindTimerGameScheduler")
    @Profile("!test")
    public TaskScheduler blindTimerGameScheduler(GameTaskSchedulerFactory schedulerFactory) {
        return schedulerFactory.create("blind-timer-", 2, "블라인드 타이머 스케줄 실행 중 예외가 발생했습니다.");
    }
}
