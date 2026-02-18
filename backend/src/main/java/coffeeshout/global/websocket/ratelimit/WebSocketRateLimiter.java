package coffeeshout.global.websocket.ratelimit;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WebSocket 세션별 메시지 Rate Limiter
 * <p>
 * Nginx에서 처리할 수 없는 WebSocket 메시지 빈도를 애플리케이션 레벨에서 제한한다.
 * HTTP Rate Limiting은 Nginx에서 처리하고, WebSocket 메시지는 여기서 처리하는 구조.
 * <p>
 * Lazy Reset 방식의 Fixed Window를 사용한다.
 * 별도의 리셋 스케줄러 없이, tryAcquire() 호출 시점에 윈도우 만료 여부를 체크하고
 * 만료됐으면 그때 리셋한다. 메시지가 안 오는 세션은 비용이 0이다.
 */
@Slf4j
@Component
public class WebSocketRateLimiter {

    private final int maxMessagesPerSecond;
    private final Clock clock;
    private final Map<String, SessionCounter> sessionCounters = new ConcurrentHashMap<>();

    @Autowired
    public WebSocketRateLimiter(
            @Value("${websocket.rate-limit.max-messages-per-second:20}") int maxMessagesPerSecond
    ) {
        this(maxMessagesPerSecond, Clock.systemUTC());
    }

    WebSocketRateLimiter(int maxMessagesPerSecond, Clock clock) {
        this.maxMessagesPerSecond = maxMessagesPerSecond;
        this.clock = clock;
    }

    /**
     * 메시지 전송 허용 여부를 판단한다.
     * 윈도우가 만료됐으면 카운터를 리셋하고, 이후 카운트를 증가시킨다.
     *
     * @param sessionId WebSocket 세션 ID
     * @return true: 허용, false: 제한 초과
     */
    public boolean tryAcquire(String sessionId) {
        final SessionCounter counter = sessionCounters.computeIfAbsent(
                sessionId, k -> new SessionCounter(clock.millis())
        );
        return counter.tryAcquire(maxMessagesPerSecond, clock.millis());
    }

    /**
     * 세션 제거 (연결 해제 시 호출)
     */
    public void removeSession(String sessionId) {
        sessionCounters.remove(sessionId);
    }

    /**
     * 비활성 세션 정리 (메모리 누수 방지)
     * 30초 이상 메시지가 없는 세션의 카운터를 제거한다.
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

    static class SessionCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart;
        private volatile long lastAccessTime;

        SessionCounter(long now) {
            this.windowStart = new AtomicLong(now);
            this.lastAccessTime = now;
        }

        /**
         * 윈도우 만료 체크 + 카운트 증가를 한 번에 처리한다.
         * 1초가 지났으면 카운터를 리셋하고, 이후 카운트를 증가시킨다.
         */
        boolean tryAcquire(int limit, long now) {
            lastAccessTime = now;

            // 윈도우 만료 시 리셋 (Lazy Reset)
            if (now - windowStart.get() >= 1000) {
                count.set(0);
                windowStart.set(now);
            }

            return count.incrementAndGet() <= limit;
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}
