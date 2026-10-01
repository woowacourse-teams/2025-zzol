package coffeeshout.config;

import java.time.Clock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
@Import(GameSchedulerTestConfig.class)
public class ServiceTestConfig {

    @Bean
    @Primary
    public SimpMessagingTemplate mockMessagingTemplate() {
        return Mockito.mock(SimpMessagingTemplate.class);
    }

    /**
     * 테스트 시계의 표준시간대는 JVM 기본값이다.
     *
     * <p>{@code systemUTC()} 였는데 바꿨다. 조회 서비스는 이 시계의 표준시간대로 "오늘"의
     * 경계를 잡는데, 엔티티는 {@code LocalDateTime.now()} 로 JVM 기본 표준시간대의 시각을
     * 적는다. 둘이 다르면 방금 만든 방이 "오늘"에서 빠진다. KST 로 도는 기계에서는
     * 자정부터 아침 아홉 시까지 아홉 시간 동안 그랬다.
     *
     * <p>UTC 로 못 박는 대신 기본값을 따라가게 두면 어느 표준시간대의 기계에서 돌리든
     * 두 시각이 같은 날을 가리킨다.
     */
    @Bean
    @Primary
    public Clock testClock() {
        return Clock.systemDefaultZone();
    }
}
