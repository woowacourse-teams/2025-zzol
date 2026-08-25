package coffeeshout.wormgame.config;

import coffeeshout.game.scheduler.GameTaskSchedulerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 전용 스케줄러. 테스트 미러 3곳(game testFixtures GameSchedulerTestConfig · game IntegrationTestConfig ·
 * app IntegrationTestConfig)에 같은 이름의 빈이 있어야 한다 — postmortem 0004.
 */
@Configuration
@EnableScheduling
public class WormGameSchedulerConfig {

    @Bean(name = "wormGameScheduler")
    @Profile("!test")
    public TaskScheduler wormGameScheduler(GameTaskSchedulerFactory schedulerFactory) {
        return schedulerFactory.create("worm-game");
    }
}
