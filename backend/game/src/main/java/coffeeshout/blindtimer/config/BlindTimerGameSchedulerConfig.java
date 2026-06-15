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
        return schedulerFactory.create("blind-timer");
    }
}
