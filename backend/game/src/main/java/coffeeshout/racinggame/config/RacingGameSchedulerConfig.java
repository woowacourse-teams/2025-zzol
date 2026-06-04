package coffeeshout.racinggame.config;

import coffeeshout.game.scheduler.GameTaskSchedulerFactory;
import coffeeshout.racinggame.domain.TapPerSecondSpeedCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RacingGameSchedulerConfig {

    @Bean
    public TapPerSecondSpeedCalculator tapPerSecondSpeedCalculator() {
        return new TapPerSecondSpeedCalculator();
    }

    @Bean(name = "racingGameScheduler")
    @Profile("!test")
    public TaskScheduler racingGameScheduler(GameTaskSchedulerFactory schedulerFactory) {
        return schedulerFactory.create("racing-game-", 3, "스케줄 실행 중 예외가 발생했습니다.");
    }
}
