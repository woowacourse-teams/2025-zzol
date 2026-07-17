package coffeeshout.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

@ExtendWith(MockitoExtension.class)
class RedisStreamContainerRegistryTest {

    @Mock
    private StreamMessageListenerContainer<?, ?> container;

    @Mock
    private StreamMessageListenerContainer<?, ?> anotherContainer;

    private RedisStreamContainerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RedisStreamContainerRegistry();
    }

    @Test
    void 등록한_컨테이너를_스트림_키로_찾는다() {
        registry.register("room", container);

        assertThat(registry.find("room")).containsSame(container);
    }

    @Test
    void 등록되지_않은_키는_빈_Optional을_반환한다() {
        assertThat(registry.find("unknown")).isEmpty();
    }

    @Test
    void 빈_파괴_시점에_등록된_모든_컨테이너를_정지한다() {
        // refresh 실패로 ContextClosedEvent 없이 파괴되는 경우에도 폴링을 확정적으로 멈춘다 (ADR-0022)
        registry.register("room", container);
        registry.register("minigame", anotherContainer);

        registry.stopAll();

        verify(container).stop();
        verify(anotherContainer).stop();
    }

    @Test
    void 한_컨테이너의_정지_실패가_나머지_컨테이너_정지를_막지_않는다() {
        registry.register("room", container);
        registry.register("minigame", anotherContainer);
        doThrow(new IllegalStateException("stop 실패")).when(container).stop();

        assertThatCode(() -> registry.stopAll()).doesNotThrowAnyException();

        verify(container).stop();
        verify(anotherContainer).stop();
    }

    @Test
    void lifecycle_stop_시_등록된_모든_컨테이너를_정지한다() {
        // 컨텍스트 pause(SF7)와 정상 종료의 lifecycle stop 양쪽이 이 경로를 탄다
        registry.register("room", container);
        registry.register("minigame", anotherContainer);

        registry.stop();

        verify(container).stop();
        verify(anotherContainer).stop();
    }

    @Test
    void lifecycle_start_시_등록된_모든_컨테이너를_재시작한다() {
        // 컨텍스트 pause 해제 시 lifecycle processor가 auto-startup 빈으로 재시작한다
        registry.register("room", container);
        registry.register("minigame", anotherContainer);

        registry.start();

        verify(container).start();
        verify(anotherContainer).start();
    }

    @Test
    void 한_컨테이너의_시작_실패가_나머지_컨테이너_시작을_막지_않는다() {
        registry.register("room", container);
        registry.register("minigame", anotherContainer);
        doThrow(new IllegalStateException("start 실패")).when(container).start();

        assertThatCode(() -> registry.start()).doesNotThrowAnyException();

        verify(container).start();
        verify(anotherContainer).start();
    }

    @Test
    void 실행_중인_컨테이너가_하나라도_있으면_running이다() {
        // mock 기본값이 false이므로 room 컨테이너는 스텁 없이 정지 상태다 (anyMatch 단락 평가로 미호출 가능)
        registry.register("room", container);
        registry.register("minigame", anotherContainer);
        given(anotherContainer.isRunning()).willReturn(true);

        assertThat(registry.isRunning()).isTrue();
    }

    @Test
    void 모든_컨테이너가_정지_상태면_running이_아니다() {
        registry.register("room", container);
        given(container.isRunning()).willReturn(false);

        assertThat(registry.isRunning()).isFalse();
    }

    @Test
    void 커넥션_팩토리보다_늦게_시작하고_먼저_정지하도록_phase가_더_높다() {
        // pause/종료 시 폴러가 팩토리보다 먼저 멈춰야 정지된 팩토리에 폴링하지 않는다
        final LettuceConnectionFactory factory = new LettuceConnectionFactory();

        assertThat(registry.getPhase()).isGreaterThan(factory.getPhase());
    }

    @Test
    void 웹_트래픽_드레인보다_늦게_정지하도록_phase가_드레인_단계들보다_낮다() {
        // 드레인 중 수신된 커맨드의 소비·브로드캐스트를 유지한다. 상수는 package-private이라 값으로 고정:
        // WebSocketGracefulShutdownHandler(MAX-1) > WebServerGracefulShutdownLifecycle(MAX-1024)
        // > WebServerStartStopLifecycle(MAX-2048) > 레지스트리
        assertThat(registry.getPhase()).isLessThan(Integer.MAX_VALUE - 2048);
    }
}
