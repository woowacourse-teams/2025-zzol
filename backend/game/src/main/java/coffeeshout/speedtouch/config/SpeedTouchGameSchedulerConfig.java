package coffeeshout.speedtouch.config;

import coffeeshout.game.scheduler.GameTaskSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;

@Configuration
public class SpeedTouchGameSchedulerConfig {

    @Bean(name = "speedTouchGameScheduler")
    @Profile("!test")
    public TaskScheduler speedTouchGameScheduler(GameTaskSchedulerFactory schedulerFactory) {
        return schedulerFactory.create("speed-touch-", 2, "스피드 터치 스케줄 실행 중 예외가 발생했습니다.");
    }
}
