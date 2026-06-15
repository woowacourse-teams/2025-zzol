package coffeeshout.friend.application;

import coffeeshout.friend.config.FriendPresenceProperties;
import coffeeshout.friend.domain.event.PresenceChangedEvent;
import coffeeshout.websocket.event.user.UserSessionConnectedEvent;
import coffeeshout.websocket.event.user.UserSessionDisconnectedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PresenceTracker {

    private final Map<Long, AtomicInteger> sessionCounts = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> pendingOffline = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final ApplicationEventPublisher eventPublisher;
    private final long gracePeriodSeconds;

    public PresenceTracker(
            @Qualifier("presenceScheduler") ScheduledExecutorService scheduler,
            ApplicationEventPublisher eventPublisher,
            FriendPresenceProperties properties
    ) {
        this.scheduler = scheduler;
        this.eventPublisher = eventPublisher;
        this.gracePeriodSeconds = properties.gracePeriodSeconds();
    }

    @EventListener
    public void onConnected(UserSessionConnectedEvent event) {
        final Long userId = event.userId();

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
    public void onDisconnected(UserSessionDisconnectedEvent event) {
        final Long userId = event.userId();

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
        final ScheduledFuture<?> previous = pendingOffline.put(userId, future);
        if (previous != null) {
            previous.cancel(false);
        }
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
}
