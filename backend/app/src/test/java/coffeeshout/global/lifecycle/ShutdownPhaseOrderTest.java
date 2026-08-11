package coffeeshout.global.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import coffeeshout.support.app.IntegrationTestSupport;
import coffeeshout.websocket.lifecycle.WebSocketGracefulShutdownHandler;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.ClassUtils;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

/**
 * 종료 시 {@link SmartLifecycle} 빈이 멈추는 순서를 고정한다.
 *
 * <p>순서표의 절반은 우리가 소유하지 않은 phase다(Spring Boot의 웹서버 라이프사이클,
 * spring-data-redis의 커넥션 팩토리). 그 값이 바뀌면 각 클래스의 주석은 조용히 거짓말하지만
 * 이 테스트는 깨진다 — 우리 값만 모은 상수 클래스로는 잡을 수 없는 드리프트를 잡는 것이 존재 이유다.
 * 실제로 {@code WebSocketGracefulShutdownHandler}의 주석이 {@code WebServerGracefulShutdownLifecycle}을
 * {@code MAX_VALUE}로 적고 있었으나 실제 값은 {@code MAX-1024}였다(#1642).</p>
 *
 * <p>기대 순서의 근거는 docs/architecture.md "종료 순서 (lifecycle phase)"에 있다.
 * 표를 고치면 이 테스트도 같이 고친다.</p>
 */
class ShutdownPhaseOrderTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("종료는 HTTP 요청 → 웹서버 → Stream 폴러 → 게이지 → Redis 커넥션 순으로 멈춘다")
    void 컨텍스트의_lifecycle_빈이_문서화된_phase_순서를_따른다() {
        // when: phase 내림차순 = stop 호출 순서
        List<String> stopOrder = stopOrder();

        // then: 사이에 다른 빈이 끼는 것은 무방하나, 이 다섯의 상대 순서는 계약이다
        assertThat(stopOrder).containsSubsequence(
                "WebServerGracefulShutdownLifecycle",
                "WebServerStartStopLifecycle",
                "RedisStreamContainerRegistry",
                "RedisStreamLagMetricService",
                "LettuceConnectionFactory"
        );
    }

    @Test
    @DisplayName("WS 세션 드레인은 HTTP 요청 드레인보다 먼저 멈춘다")
    void WS_드레인_핸들러가_웹서버_드레인보다_먼저_멈춘다() {
        // WebSocketGracefulShutdownHandler 는 @Profile("!test") 라 테스트 컨텍스트에 없다.
        // getPhase() 는 상수 반환이므로 협력자 없이 값만 비교한다
        int webSocketDrain = webSocketShutdownHandler().getPhase();

        // then: 높은 phase 가 먼저 멈춘다. HTTP 요청 드레인이 시작되기 전에 WS 세션을 정리해야 한다
        assertThat(webSocketDrain)
                .isGreaterThan(WebServerApplicationContext.GRACEFUL_SHUTDOWN_PHASE);
    }

    private WebSocketGracefulShutdownHandler webSocketShutdownHandler() {
        return new WebSocketGracefulShutdownHandler(
                mock(WebSocketMessageBrokerStats.class),
                mock(TaskScheduler.class),
                Duration.ofMinutes(5)
        );
    }

    private List<String> stopOrder() {
        return applicationContext.getBeansOfType(SmartLifecycle.class).values().stream()
                .sorted(Comparator.comparingInt(SmartLifecycle::getPhase).reversed())
                .map(bean -> ClassUtils.getUserClass(bean).getSimpleName())
                .toList();
    }
}
