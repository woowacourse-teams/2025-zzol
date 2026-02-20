package coffeeshout.global.health;

import static coffeeshout.global.redis.config.RedisStreamListenerStarter.STREAM_CONTAINER_BEAN_NAME_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coffeeshout.global.redis.config.RedisStreamProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

@ExtendWith(MockitoExtension.class)
class RedisStreamContainerRecoveryTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private RedisStreamProperties redisStreamProperties;

    @Mock
    private StreamMessageListenerContainer<?, ?> runningContainer;

    @Mock
    private StreamMessageListenerContainer<?, ?> stoppedContainer;

    private RedisStreamContainerRecovery recovery;

    private static final String[] STREAM_KEYS = {
            "room", "room:join", "cardgame:select", "minigame", "racinggame"
    };

    @BeforeEach
    void setUp() {
        recovery = new RedisStreamContainerRecovery(applicationContext, redisStreamProperties);

        Map<String, RedisStreamProperties.StreamConfig> keys = new LinkedHashMap<>();
        for (String key : STREAM_KEYS) {
            keys.put(key, null);
        }
        given(redisStreamProperties.keys()).willReturn(keys);
    }

    private void mockAllContainersRunning() {
        given(runningContainer.isRunning()).willReturn(true);
        for (String streamKey : STREAM_KEYS) {
            String beanName = String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey);
            given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                    .willReturn(runningContainer);
        }
    }

    @Test
    void 모든_컨테이너가_정상이면_복구_불가_스트림이_없다() {
        mockAllContainersRunning();

        recovery.checkAndRecover();

        assertThat(recovery.hasUnrecoverableStreams()).isFalse();
    }

    @Test
    void 멈춘_컨테이너를_발견하면_start를_호출한다() {
        given(stoppedContainer.isRunning()).willReturn(false, true);
        given(runningContainer.isRunning()).willReturn(true);

        for (String streamKey : STREAM_KEYS) {
            String beanName = String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey);
            if (streamKey.equals("room")) {
                given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                        .willReturn(stoppedContainer);
            } else {
                given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                        .willReturn(runningContainer);
            }
        }

        recovery.checkAndRecover();

        verify(stoppedContainer).start();
        assertThat(recovery.hasUnrecoverableStreams()).isFalse();
    }

    @Test
    void 복구_시도_1회_실패하면_아직_DOWN이_아니다() {
        given(stoppedContainer.isRunning()).willReturn(false);
        given(runningContainer.isRunning()).willReturn(true);

        for (String streamKey : STREAM_KEYS) {
            String beanName = String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey);
            if (streamKey.equals("room")) {
                given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                        .willReturn(stoppedContainer);
            } else {
                given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                        .willReturn(runningContainer);
            }
        }

        recovery.checkAndRecover();

        assertThat(recovery.hasUnrecoverableStreams()).isFalse();
    }

    @Test
    void 복구_시도_2회_연속_실패하면_복구_불가로_판정한다() {
        given(stoppedContainer.isRunning()).willReturn(false);
        given(runningContainer.isRunning()).willReturn(true);

        for (String streamKey : STREAM_KEYS) {
            String beanName = String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey);
            if (streamKey.equals("room")) {
                given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                        .willReturn(stoppedContainer);
            } else {
                given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                        .willReturn(runningContainer);
            }
        }

        recovery.checkAndRecover();
        recovery.checkAndRecover();

        assertThat(recovery.hasUnrecoverableStreams()).isTrue();
        assertThat(recovery.getFailedRecoveryStreams()).contains("room");
    }

    @Test
    void 복구_성공하면_실패_카운트가_초기화된다() {
        given(stoppedContainer.isRunning()).willReturn(false);
        given(runningContainer.isRunning()).willReturn(true);

        for (String streamKey : STREAM_KEYS) {
            String beanName = String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey);
            if (streamKey.equals("room")) {
                given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                        .willReturn(stoppedContainer);
            } else {
                given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                        .willReturn(runningContainer);
            }
        }

        recovery.checkAndRecover(); // 1회 실패

        // 2회차: 복구 성공
        given(stoppedContainer.isRunning()).willReturn(true);

        recovery.checkAndRecover();

        assertThat(recovery.hasUnrecoverableStreams()).isFalse();
    }
}
