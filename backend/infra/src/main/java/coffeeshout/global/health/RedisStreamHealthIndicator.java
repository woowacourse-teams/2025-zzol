package coffeeshout.global.health;

import static coffeeshout.global.redis.config.RedisStreamListenerStarter.STREAM_CONTAINER_BEAN_NAME_FORMAT;

import coffeeshout.global.redis.config.RedisStreamProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Redis Stream Listener Container 상태 기반 헬스체크.
 * <p>
 * 이 HealthIndicator는 상태를 "보고"만 하고, "복구"는 하지 않는다.
 * 복구는 {@link RedisStreamContainerRecovery}가 담당한다.
 * <p>
 * DOWN 조건: Recovery가 복구를 시도했지만 실패한 container가 있을 때.
 * 즉, 애플리케이션 내부에서 복구를 시도한 후에도 안 되는 경우에만
 * Docker 재시작(last resort)을 트리거한다.
 * <p>
 * 스트림 키 목록은 RedisStreamProperties에서 동적으로 로드한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamHealthIndicator implements HealthIndicator {

    private final ApplicationContext applicationContext;
    private final RedisStreamContainerRecovery containerRecovery;
    private final RedisStreamProperties redisStreamProperties;

    @Override
    public Health health() {
        final Map<String, Object> details = new HashMap<>();

        if (redisStreamProperties.keys() != null) {
            for (String streamKey : redisStreamProperties.keys().keySet()) {
                final String beanName = String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey);

                try {
                    final StreamMessageListenerContainer<?, ?> container =
                            applicationContext.getBean(beanName, StreamMessageListenerContainer.class);

                    final boolean running = container.isRunning();
                    details.put(streamKey, running ? "RUNNING" : "STOPPED");
                } catch (NoSuchBeanDefinitionException e) {
                    details.put(streamKey, "NOT_REGISTERED");
                }
            }
        }

        if (containerRecovery.hasUnrecoverableStreams()) {
            final Set<String> failedStreams = containerRecovery.getFailedRecoveryStreams();
            details.put("unrecoverable", failedStreams);
            details.put("action", "Internal recovery failed. Docker restart required.");
            return Health.down()
                    .withDetails(details)
                    .build();
        }

        return Health.up()
                .withDetails(details)
                .build();
    }
}
