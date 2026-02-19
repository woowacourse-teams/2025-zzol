package coffeeshout.global.health;

import static coffeeshout.global.redis.config.RedisStreamListenerStarter.STREAM_CONTAINER_BEAN_NAME_FORMAT;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Redis Stream Listener Container 상태 기반 헬스체크.
 * <p>
 * 각 스트림의 ListenerContainer가 running 상태인지 확인한다.
 * Container가 멈춰있으면 이벤트 소비가 중단된 것이고,
 * 서버 재시작으로 container가 재생성되면 복구 가능하다.
 * <p>
 * DOWN이 되면 Docker HEALTHCHECK에 의해 컨테이너 재시작이 트리거된다.
 * 재시작 시 RedisStreamListenerStarter가 모든 container를 다시 생성하므로
 * 자동 복구가 가능하다.
 */
@Slf4j
@Component
public class RedisStreamHealthIndicator implements HealthIndicator {

    private static final String[] STREAM_KEYS = {
            "room", "room:join", "cardgame:select", "minigame", "racinggame"
    };

    private final ApplicationContext applicationContext;

    public RedisStreamHealthIndicator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Health health() {
        final Map<String, Object> details = new HashMap<>();
        boolean hasStoppedContainer = false;

        for (String streamKey : STREAM_KEYS) {
            final String beanName = String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey);

            try {
                final StreamMessageListenerContainer<?, ?> container =
                        applicationContext.getBean(beanName, StreamMessageListenerContainer.class);

                final boolean running = container.isRunning();
                details.put(streamKey, running ? "RUNNING" : "STOPPED");

                if (!running) {
                    hasStoppedContainer = true;
                    log.warn("Redis Stream container 중단 감지: stream={}", streamKey);
                }
            } catch (Exception e) {
                details.put(streamKey, "NOT_REGISTERED");
                log.debug("Redis Stream container 빈 없음: stream={}, reason={}", streamKey, e.getMessage());
            }
        }

        if (hasStoppedContainer) {
            return Health.down()
                    .withDetails(details)
                    .build();
        }

        return Health.up()
                .withDetails(details)
                .build();
    }
}
