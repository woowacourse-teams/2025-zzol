package coffeeshout.global.redis.config;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * 스트림 키별 ListenerContainer의 단일 장부.
 * <p>
 * Spring 컨테이너에 동적 빈으로 등록하던 기존 방식(문자열 빈 이름 + getBean 조회)을 대체한다.
 * 등록은 {@link RedisStreamListenerStarter}, 조회는 Recovery/HealthIndicator가 담당한다.
 */
@Slf4j
@Component
public class RedisStreamContainerRegistry {

    private final Map<String, StreamMessageListenerContainer<?, ?>> containers = new ConcurrentHashMap<>();

    public void register(String streamKey, StreamMessageListenerContainer<?, ?> container) {
        containers.put(streamKey, container);
    }

    public Optional<StreamMessageListenerContainer<?, ?>> find(String streamKey) {
        return Optional.ofNullable(containers.get(streamKey));
    }

    // refresh 실패 시에는 ContextClosedEvent가 발행되지 않아 이미 시작된 컨테이너가
    // 파괴된 커넥션 팩토리에 무한 폴링한다. @PreDestroy는 정상 종료와 refresh 실패
    // 양쪽에서 호출되므로 여기서 확정적으로 멈춘다 (stop()은 멱등).
    // 빈 파괴는 역의존 순서이므로 Starter의 stopping 플래그 설정이 항상 이보다 먼저 실행된다 (ADR-0022)
    // package-private: 라이프사이클 콜백이라 외부 호출은 불필요하며, 동일 패키지 테스트 접근용으로만 연다
    @PreDestroy
    void stopAll() {
        containers.forEach(this::stopSafely);
    }

    // 한 컨테이너의 stop() 실패가 나머지 컨테이너 정지를 막지 않도록 예외를 격리한다
    private void stopSafely(String streamKey, StreamMessageListenerContainer<?, ?> container) {
        try {
            container.stop();
        } catch (Exception e) {
            log.warn("Redis Stream 컨테이너 정지 실패: streamKey={}", streamKey, e);
        }
    }
}
