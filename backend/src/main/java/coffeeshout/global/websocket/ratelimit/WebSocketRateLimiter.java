package coffeeshout.global.websocket.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WebSocket 세션별 메시지 Rate Limiter
 * <p>
 * Nginx에서 처리할 수 없는 WebSocket 메시지 빈도를 애플리케이션 레벨에서 제한한다. HTTP Rate Limiting은 Nginx에서 처리하고, WebSocket 메시지는 여기서 처리하는 구조.
 * <p>
 * Lazy Reset 방식의 Fixed Window를 사용한다. 별도의 리셋 스케줄러 없이, tryAcquire() 호출 시점에 윈도우 만료 여부를 체크하고 만료됐으면 그때 리셋한다. 메시지가 안 오는 세션은
 * 비용이 0이다.
 * <p>
 * Fixed Window의 알려진 한계로, 윈도우 경계에서 순간적으로 2 × limit 메시지가 통과할 수 있다. (예: t=0.999s에 20건, t=1.000s에 20건 → 0.001초 사이에 40건 통과)
 * 이 서비스에서 정상 사용자는 초당 1.3건이고, 비정상 스크립트를 차단하는 것이 목적이므로 경계 버스트로 인한 실질적 위협은 없다고 판단했다. 더 엄격한 제어가 필요하면 Sliding Window Log 또는
 * Token Bucket 방식을 고려할 수 있다.
 */
@Slf4j
@Component
public class WebSocketRateLimiter {

    private final int maxMessagesPerSecond;
    private final Clock clock;
    private final Counter rateLimitDropCounter;
    private final Map<String, SessionCounter> sessionCounters = new ConcurrentHashMap<>();

    @Autowired
    public WebSocketRateLimiter(
            @Value("${websocket.rate-limit.max-messages-per-second:20}") int maxMessagesPerSecond,
            MeterRegistry meterRegistry
    ) {
        this(maxMessagesPerSecond, Clock.systemUTC(), meterRegistry);
    }

    WebSocketRateLimiter(int maxMessagesPerSecond, Clock clock) {
        this(maxMessagesPerSecond, clock, null);
    }

    WebSocketRateLimiter(int maxMessagesPerSecond, Clock clock, MeterRegistry meterRegistry) {
        this.maxMessagesPerSecond = maxMessagesPerSecond;
        this.clock = clock;
        if (meterRegistry != null) {
            this.rateLimitDropCounter = Counter.builder("websocket.ratelimit.dropped.total")
                    .description("WebSocket Rate Limit에 의해 드롭된 메시지 수")
                    .register(meterRegistry);
        } else {
            this.rateLimitDropCounter = null;
        }
    }

    /**
     * 메시지 전송 허용 여부를 판단한다. 윈도우가 만료됐으면 카운터를 리셋하고, 이후 카운트를 증가시킨다.
     *
     * @param sessionId WebSocket 세션 ID
     * @return true: 허용, false: 제한 초과
     */
    public boolean tryAcquire(String sessionId) {
        final SessionCounter counter = sessionCounters.computeIfAbsent(
                sessionId, k -> new SessionCounter(clock.millis())
        );
        boolean allowed = counter.tryAcquire(maxMessagesPerSecond, clock.millis());
        if (!allowed && rateLimitDropCounter != null) {
            rateLimitDropCounter.increment();
        }
        return allowed;
    }

    /**
     * 세션 제거 (연결 해제 시 호출)
     */
    public void removeSession(String sessionId) {
        sessionCounters.remove(sessionId);
    }

    /**
     * 비활성 세션 정리 (메모리 누수 방지) 30초 이상 메시지가 없는 세션의 카운터를 제거한다.
     */
    @Scheduled(fixedRate = 30_000)
    public void cleanupInactiveSessions() {
        final long now = clock.millis();
        final int before = sessionCounters.size();

        sessionCounters.entrySet().removeIf(entry ->
                now - entry.getValue().getLastAccessTime() > 30_000
        );

        final int removed = before - sessionCounters.size();
        if (removed > 0) {
            log.debug("비활성 WebSocket Rate Limit 세션 정리: {} 개 제거, 남은 세션: {} 개",
                    removed, sessionCounters.size());
        }
    }

    /**
     * 현재 추적 중인 세션 수 (모니터링용)
     */
    public int getTrackedSessionCount() {
        return sessionCounters.size();
    }

    /**
     * 세션별 메시지 카운터.
     * <p>
     * synchronized로 "윈도우 만료 체크 → 리셋 → 카운트 증가"를 하나의 임계 구역으로 묶는다. SessionCounter는 세션 단위 인스턴스이므로, 락 경합 범위가 해당 세션에 한정된다. 다른
     * 세션의 처리에는 영향을 주지 않는다.
     */
    static class SessionCounter {
        private int count;
        private long windowStart;
        private long lastAccessTime;

        SessionCounter(long now) {
            this.count = 0;
            this.windowStart = now;
            this.lastAccessTime = now;
        }

        /**
         * 윈도우 만료 체크 + 리셋 + 카운트 증가를 원자적으로 처리한다.
         */
        synchronized boolean tryAcquire(int limit, long now) {
            lastAccessTime = now;

            if (now - windowStart >= 1000) {
                count = 0;
                windowStart = now;
            }

            return ++count <= limit;
        }

        synchronized long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}
