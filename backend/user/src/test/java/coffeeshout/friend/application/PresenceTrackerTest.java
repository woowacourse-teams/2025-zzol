package coffeeshout.friend.application;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.friend.config.FriendPresenceProperties;
import coffeeshout.friend.domain.event.PresenceChangedEvent;
import coffeeshout.websocket.event.user.UserSessionConnectedEvent;
import coffeeshout.websocket.event.user.UserSessionDisconnectedEvent;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class PresenceTrackerTest {

    ApplicationEventPublisher eventPublisher;
    ScheduledExecutorService scheduler;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        scheduler = newScheduler();
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
    }

    private ScheduledExecutorService newScheduler() {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    private PresenceTracker trackerWithGrace(long gracePeriodSeconds) {
        return new PresenceTracker(scheduler, eventPublisher, new FriendPresenceProperties(gracePeriodSeconds));
    }

    @Nested
    class 세션_연결 {

        @Test
        void 첫_번째_연결_시_온라인_이벤트를_발행한다() {
            final PresenceTracker tracker = trackerWithGrace(10);

            tracker.onConnected(connected(1L));

            final ArgumentCaptor<PresenceChangedEvent> captor = forClass(PresenceChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().online()).isTrue();
        }

        @Test
        void 두_번째_연결_시_온라인_이벤트를_중복_발행하지_않는다() {
            final PresenceTracker tracker = trackerWithGrace(10);

            tracker.onConnected(connected(1L));
            tracker.onConnected(connected(1L));

            verify(eventPublisher, times(1)).publishEvent((Object) any());
        }

        @Test
        void 연결_후_온라인_상태가_된다() {
            final PresenceTracker tracker = trackerWithGrace(10);

            tracker.onConnected(connected(1L));

            assertThat(tracker.isOnline(1L)).isTrue();
        }

        @Test
        void 연결_이력이_없으면_오프라인이다() {
            final PresenceTracker tracker = trackerWithGrace(10);

            assertThat(tracker.isOnline(999L)).isFalse();
        }
    }

    @Nested
    class 세션_종료 {

        @Test
        void 남은_세션이_있으면_오프라인_이벤트를_발행하지_않는다() {
            final PresenceTracker tracker = trackerWithGrace(10);
            tracker.onConnected(connected(1L));
            tracker.onConnected(connected(1L));

            tracker.onDisconnected(disconnected(1L));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tracker.isOnline(1L)).isTrue();
                softly.assertThatCode(() ->
                        verify(eventPublisher, times(1)).publishEvent((Object) any())
                ).doesNotThrowAnyException();
            });
        }

        @Test
        void 마지막_세션_종료_후_유예_시간이_지나면_오프라인_이벤트를_발행한다() {
            final PresenceTracker tracker = trackerWithGrace(1);
            tracker.onConnected(connected(1L));

            tracker.onDisconnected(disconnected(1L));

            final ArgumentCaptor<PresenceChangedEvent> captor = forClass(PresenceChangedEvent.class);
            await().atMost(ofSeconds(3)).untilAsserted(() -> {
                verify(eventPublisher, times(2)).publishEvent(captor.capture());
                assertThat(captor.getAllValues())
                        .filteredOn(e -> !e.online())
                        .hasSize(1);
            });
        }

        @Test
        void 마지막_세션_종료_후_유예_시간이_지나면_오프라인_상태가_된다() {
            final PresenceTracker tracker = trackerWithGrace(1);
            tracker.onConnected(connected(1L));

            tracker.onDisconnected(disconnected(1L));

            await().atMost(ofSeconds(3)).until(() -> !tracker.isOnline(1L));
        }
    }

    @Nested
    class 유예_시간_내_재연결 {

        @Test
        void 재연결_시_오프라인_이벤트를_발행하지_않는다() {
            final PresenceTracker tracker = trackerWithGrace(10);
            tracker.onConnected(connected(1L));
            tracker.onDisconnected(disconnected(1L));

            tracker.onConnected(connected(1L));

            verify(eventPublisher, never()).publishEvent(
                    argThat((Object obj) -> obj instanceof PresenceChangedEvent p && !p.online())
            );
        }

        @Test
        void 재연결_후_온라인_상태를_유지한다() {
            final PresenceTracker tracker = trackerWithGrace(10);
            tracker.onConnected(connected(1L));
            tracker.onDisconnected(disconnected(1L));

            tracker.onConnected(connected(1L));

            assertThat(tracker.isOnline(1L)).isTrue();
        }
    }

    private static UserSessionConnectedEvent connected(Long userId) {
        return new UserSessionConnectedEvent(userId, "session-" + userId);
    }

    private static UserSessionDisconnectedEvent disconnected(Long userId) {
        return new UserSessionDisconnectedEvent(userId, "session-" + userId);
    }
}
