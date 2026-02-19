package coffeeshout.global.health;

import static coffeeshout.global.redis.config.RedisStreamListenerStarter.STREAM_CONTAINER_BEAN_NAME_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

@ExtendWith(MockitoExtension.class)
class RedisStreamHealthIndicatorTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private StreamMessageListenerContainer<?, ?> runningContainer;

    @Mock
    private StreamMessageListenerContainer<?, ?> stoppedContainer;

    private RedisStreamHealthIndicator indicator;

    private static final String[] STREAM_KEYS = {
            "room", "room:join", "cardgame:select", "minigame", "racinggame"
    };

    @BeforeEach
    void setUp() {
        indicator = new RedisStreamHealthIndicator(applicationContext);
    }

    @Test
    void 모든_컨테이너가_실행_중이면_UP을_반환한다() {
        given(runningContainer.isRunning()).willReturn(true);

        for (String streamKey : STREAM_KEYS) {
            String beanName = String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey);
            given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                    .willReturn(runningContainer);
        }

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("room", "RUNNING");
    }

    @Test
    void 하나라도_중단된_컨테이너가_있으면_DOWN을_반환한다() {
        given(runningContainer.isRunning()).willReturn(true);
        given(stoppedContainer.isRunning()).willReturn(false);

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

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("room", "STOPPED");
    }

    @Test
    void 컨테이너_빈이_없으면_NOT_REGISTERED로_표시하고_UP을_반환한다() {
        for (String streamKey : STREAM_KEYS) {
            String beanName = String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey);
            given(applicationContext.getBean(beanName, StreamMessageListenerContainer.class))
                    .willThrow(new org.springframework.beans.factory.NoSuchBeanDefinitionException(beanName));
        }

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("room", "NOT_REGISTERED");
    }
}
