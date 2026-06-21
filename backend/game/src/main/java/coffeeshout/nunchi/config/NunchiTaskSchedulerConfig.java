package coffeeshout.nunchi.config;

import coffeeshout.game.scheduler.GameTaskSchedulerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 눈치게임 전용 스케줄러 설정. 윈도우(300ms)·쿨다운·idle·하드캡 타이머를 동적으로
 * cancel·reschedule해야 하므로(ADR-0031 N2, 단발·취소불가인 {@code FlowScheduler} SPI로는 표현
 * 불가), Flow 오케스트레이터가 raw {@link ThreadPoolTaskScheduler}를 직접 주입받는다
 * (SpeedTouch/BlindTimer 선례). 전용 풀이라 다른 게임 타이머와 자원을 다투지 않는다.
 */
@Configuration
@EnableConfigurationProperties(NunchiTimingProperties.class)
public class NunchiTaskSchedulerConfig {

    @Bean(name = "nunchiGameScheduler")
    @Profile("!test")
    public ThreadPoolTaskScheduler nunchiGameScheduler(GameTaskSchedulerFactory schedulerFactory) {
        return schedulerFactory.create("nunchi");
    }
}
