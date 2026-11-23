package coffeeshout.global.redis.stream;

import coffeeshout.global.metric.RedisStreamPublishMetricService;
import coffeeshout.global.redis.config.StreamPublishProperties;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * 응답을 기다리는 Redis Stream 비동기 발행 수를 제한하고, 종료할 때 남은 발행이 끝나기를 기다린다.
 *
 * <p>비동기 발행은 호출 스레드가 XADD 응답을 기다리지 않는다. 그래서 스레드 수가 더는 발행 속도를 눌러 주지 않고,
 * 상한이 없으면 Redis가 느려질 때 Lettuce 요청 큐에 명령이 끝없이 쌓인다.</p>
 *
 * <p>종료 대기는 {@code @PreDestroy}가 아니라 {@link SmartLifecycle}로 한다. {@code @PreDestroy}는 빈 파괴 단계에서
 * 실행되는데, {@code LettuceConnectionFactory}는 그보다 앞선 lifecycle stop 단계에서 이미 커넥션을 닫는다.</p>
 */
@Slf4j
@Component
public class InFlightPublishLimiter implements SmartLifecycle {

    private final int maxInFlight;
    private final Duration drainTimeout;
    private final Semaphore permits;
    private volatile boolean running = true;

    public InFlightPublishLimiter(
            StreamPublishProperties streamPublishProperties, RedisStreamPublishMetricService publishMetricService) {
        this.maxInFlight = streamPublishProperties.maxInFlight();
        this.drainTimeout = streamPublishProperties.drainTimeout();
        this.permits = new Semaphore(maxInFlight);
        publishMetricService.bindInFlight(permits, maxInFlight);
    }

    /**
     * 응답을 기다리는 발행 자리를 하나 잡는다. 상한에 닿았으면 잡지 않고 {@code false}를 돌려준다.
     */
    public boolean tryAcquire() {
        return permits.tryAcquire();
    }

    /**
     * XADD가 성공하든 실패하든 끝나면 자리를 돌려준다.
     */
    public void release() {
        permits.release();
    }

    @Override
    public void start() {
        running = true;
    }

    /**
     * 응답을 기다리는 발행이 모두 끝나거나 {@code drainTimeout}이 지날 때까지 기다린다.
     */
    @Override
    public void stop() {
        running = false;
        awaitInFlight();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    // 웹서버 stop(MAX-2048) 뒤라 새 발행이 더 들어오지 않는다. Stream 폴러 정지(1024)와 커넥션 팩토리 정지(0)보다는
    // 앞이라, 남은 XADD가 커넥션이 닫히기 전에 끝나고 이 인스턴스의 컨슈머도 아직 받는다.
    // 전체 순서표는 docs/architecture.md "종료 순서 (lifecycle phase)"
    @Override
    public int getPhase() {
        return 2048;
    }

    private void awaitInFlight() {
        try {
            if (permits.tryAcquire(maxInFlight, drainTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                // 남은 발행이 없는지 확인하려고 잡은 자리다. 테스트 컨텍스트처럼 멈췄다가 다시 시작하는 경우를 위해 돌려준다
                permits.release(maxInFlight);
                return;
            }
            log.warn("종료 전에 끝나지 않은 Redis Stream 비동기 발행이 있습니다: {}건", maxInFlight - permits.availablePermits());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
