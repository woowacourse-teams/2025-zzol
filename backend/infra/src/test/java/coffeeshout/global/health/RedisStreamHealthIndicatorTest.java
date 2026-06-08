package coffeeshout.global.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import coffeeshout.global.redis.config.RedisStreamContainerRegistry;
import coffeeshout.global.redis.config.RedisStreamProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

@ExtendWith(MockitoExtension.class)
class RedisStreamHealthIndicatorTest {

    @Mock
    private RedisStreamContainerRecovery containerRecovery;

    @Mock
    private RedisStreamProperties redisStreamProperties;

    @Mock
    private StreamMessageListenerContainer<?, ?> runningContainer;

    @Mock
    private StreamMessageListenerContainer<?, ?> stoppedContainer;

    private RedisStreamContainerRegistry containerRegistry;
    private RedisStreamHealthIndicator indicator;

    private static final String[] STREAM_KEYS = {
            "room", "room:join", "cardgame:select", "minigame", "racinggame"
    };

    @BeforeEach
    void setUp() {
        containerRegistry = new RedisStreamContainerRegistry();
        indicator = new RedisStreamHealthIndicator(containerRegistry, containerRecovery, redisStreamProperties);

        Map<String, RedisStreamProperties.StreamConfig> keys = new LinkedHashMap<>();
        for (String key : STREAM_KEYS) {
            keys.put(key, null);
        }
        given(redisStreamProperties.keys()).willReturn(keys);
    }

    private void registerAllRunning() {
        given(runningContainer.isRunning()).willReturn(true);
        for (String streamKey : STREAM_KEYS) {
            containerRegistry.register(streamKey, runningContainer);
        }
    }

    @Test
    void 모든_컨테이너가_실행_중이고_복구_실패가_없으면_UP을_반환한다() {
        registerAllRunning();
        given(containerRecovery.hasUnrecoverableStreams()).willReturn(false);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("room", "RUNNING");
    }

    @Test
    void 컨테이너가_중단됐지만_복구_실패가_아직_없으면_UP을_반환한다() {
        given(runningContainer.isRunning()).willReturn(true);
        given(stoppedContainer.isRunning()).willReturn(false);
        given(containerRecovery.hasUnrecoverableStreams()).willReturn(false);

        for (String streamKey : STREAM_KEYS) {
            containerRegistry.register(streamKey, streamKey.equals("room") ? stoppedContainer : runningContainer);
        }

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("room", "STOPPED");
    }

    @Test
    void 복구_실패한_스트림이_있으면_DOWN을_반환한다() {
        registerAllRunning();
        given(containerRecovery.hasUnrecoverableStreams()).willReturn(true);
        given(containerRecovery.getFailedRecoveryStreams()).willReturn(Set.of("room"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("unrecoverable");
    }

    @Test
    void 레지스트리에_없는_컨테이너는_NOT_REGISTERED로_표시하고_UP을_반환한다() {
        // 아무 컨테이너도 등록하지 않는다 — 기동 실패 등으로 등록 전인 상태
        given(containerRecovery.hasUnrecoverableStreams()).willReturn(false);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("room", "NOT_REGISTERED");
    }
}
