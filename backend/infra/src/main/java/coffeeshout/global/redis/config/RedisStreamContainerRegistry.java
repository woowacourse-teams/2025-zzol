package coffeeshout.global.redis.config;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * 스트림 키별 ListenerContainer의 단일 장부이자 라이프사이클 대리인.
 * <p>
 * Spring 컨테이너에 동적 빈으로 등록하던 기존 방식(문자열 빈 이름 + getBean 조회)을 대체한다.
 * 등록은 {@link RedisStreamListenerStarter}, 조회는 Recovery/HealthIndicator가 담당한다.
 * <p>
 * 컨테이너는 Starter가 수동 생성한 비(非)빈이라 Spring 라이프사이클(컨텍스트 close의 lifecycle
 * stop, Spring Framework 7 테스트 컨텍스트 pause)이 인지하지 못한다 — 커넥션 팩토리만 정지되고
 * 폴러는 살아남아 정지된 팩토리에 무한 재시도한다. 레지스트리가 {@link SmartLifecycle}로 stop/start를
 * 대신 위임받아 컨테이너를 라이프사이클에 편입시킨다.
 */
@Slf4j
@Component
public class RedisStreamContainerRegistry implements SmartLifecycle {

    private final Map<String, StreamMessageListenerContainer<?, ?>> containers = new ConcurrentHashMap<>();

    public void register(String streamKey, StreamMessageListenerContainer<?, ?> container) {
        containers.put(streamKey, container);
    }

    public Optional<StreamMessageListenerContainer<?, ?>> find(String streamKey) {
        return Optional.ofNullable(containers.get(streamKey));
    }

    // 최초 기동은 Starter의 @PostConstruct가 register→start→await로 직접 수행하므로(ADR-0022)
    // onRefresh 시점의 이 호출은 no-op이다. 실질 호출처는 테스트 컨텍스트 pause 해제(재사용) 시점의
    // lifecycle 재시작이다 — 컨테이너는 등록된 구독을 유지하므로 start()만으로 폴링이 재개된다
    @Override
    public void start() {
        containers.forEach(this::startSafely);
    }

    @Override
    public void stop() {
        containers.forEach(this::stopSafely);
    }

    // 상태 플래그를 따로 두지 않고 컨테이너에서 유도한다 — lifecycle processor는 running일 때만
    // stop을, not running일 때만 start를 호출하므로 이 값이 위임의 유일한 진실 공급원이다
    @Override
    public boolean isRunning() {
        return containers.values().stream().anyMatch(StreamMessageListenerContainer::isRunning);
    }

    // 종료(stop)는 phase 내림차순이다. 폴러는 웹 트래픽 드레인이 모두 끝난 뒤 멈춰야
    // 드레인 중 수신된 커맨드의 소비·브로드캐스트가 유지되고(WS 세션 드레인 MAX-1,
    // Tomcat 드레인 MAX-1024, 웹서버 stop MAX-2048), LettuceConnectionFactory(phase 0)보다는
    // 먼저 멈춰야 정지된 팩토리에 폴링하지 않는다. 그 사이 값이면 되고, 1024는 그 표식이다
    @Override
    public int getPhase() {
        return 1024;
    }

    // refresh 실패 시에는 ContextClosedEvent도 lifecycle stop도 없이 빈 파괴만 진행되어 이미
    // 시작된 컨테이너가 파괴된 커넥션 팩토리에 무한 폴링한다. @PreDestroy는 정상 종료와 refresh
    // 실패 양쪽에서 호출되므로 여기서 확정적으로 멈춘다 (stop()은 멱등).
    // 빈 파괴는 역의존 순서이므로 Starter의 stopping 플래그 설정이 항상 이보다 먼저 실행된다 (ADR-0022)
    // package-private: 라이프사이클 콜백이라 외부 호출은 불필요하며, 동일 패키지 테스트 접근용으로만 연다
    @PreDestroy
    void stopAll() {
        stop();
    }

    // 한 컨테이너의 start/stop 실패가 나머지 컨테이너의 처리를 막지 않도록 예외를 격리한다
    private void startSafely(String streamKey, StreamMessageListenerContainer<?, ?> container) {
        try {
            container.start();
        } catch (Exception e) {
            log.warn("Redis Stream 컨테이너 시작 실패: streamKey={}", streamKey, e);
        }
    }

    private void stopSafely(String streamKey, StreamMessageListenerContainer<?, ?> container) {
        try {
            container.stop();
        } catch (Exception e) {
            log.warn("Redis Stream 컨테이너 정지 실패: streamKey={}", streamKey, e);
        }
    }
}
