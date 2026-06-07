package coffeeshout.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}
