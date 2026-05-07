package coffeeshout.friend.application;

import coffeeshout.friend.config.FriendPresenceProperties;
import coffeeshout.friend.domain.event.PresenceChangedEvent;
import coffeeshout.global.websocket.UserPrincipal;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class PresenceTracker {

    private final Map<Long, AtomicInteger> sessionCounts = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> pendingOffline = new ConcurrentHashMap<>();
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
    private final ApplicationEventPublisher eventPublisher;
    private final long gracePeriodSeconds;

    public PresenceTracker(ApplicationEventPublisher eventPublisher, FriendPresenceProperties properties) {
        this.eventPublisher = eventPublisher;
        this.gracePeriodSeconds = properties.gracePeriodSeconds();
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        final Long userId = extractUserId(event.getMessage());
        if (userId == null) {
            return;
        }

        final ScheduledFuture<?> pending = pendingOffline.remove(userId);
        if (pending != null) {
            pending.cancel(false);
        }

        final int count = sessionCounts
                .computeIfAbsent(userId, id -> new AtomicInteger(0))
                .incrementAndGet();

        if (count == 1) {
            log.debug("사용자 온라인: userId={}", userId);
            eventPublisher.publishEvent(new PresenceChangedEvent(userId, true));
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        final Long userId = extractUserId(event.getMessage());
        if (userId == null) {
            return;
        }

        final AtomicInteger counter = sessionCounts.get(userId);
        if (counter == null) {
            return;
        }

        final int count = counter.updateAndGet(c -> Math.max(0, c - 1));
        if (count > 0) {
            return;
        }

        final ScheduledFuture<?> future = scheduler.schedule(
                () -> handleOffline(userId), gracePeriodSeconds, TimeUnit.SECONDS
        );
        pendingOffline.put(userId, future);
    }

    public boolean isOnline(Long userId) {
        final AtomicInteger counter = sessionCounts.get(userId);
        return counter != null && counter.get() > 0;
    }

    private void handleOffline(Long userId) {
        pendingOffline.remove(userId);
        final AtomicInteger counter = sessionCounts.get(userId);
        if (counter != null && counter.get() <= 0) {
            sessionCounts.remove(userId);
            log.debug("사용자 오프라인: userId={}", userId);
            eventPublisher.publishEvent(new PresenceChangedEvent(userId, false));
        }
    }

    private Long extractUserId(Message<?> message) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        final Principal principal = accessor.getUser();
        if (principal == null || !principal.getName().startsWith(UserPrincipal.PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(principal.getName().substring(UserPrincipal.PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
