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
        return schedulerFactory.create("speed-touch");
    }
}
