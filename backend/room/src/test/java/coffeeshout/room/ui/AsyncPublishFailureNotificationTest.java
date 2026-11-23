package coffeeshout.room.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import coffeeshout.RoomModuleWebSocketTest;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.ui.request.ReadyChangeMessage;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.TestStompSession.MessageCollector;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * WebSocket 핸들러가 {@link StreamPublisher#publishAsync}의 future를 반환하면,
 * XADD가 나중에 실패해도 {@code @MessageExceptionHandler}를 거쳐 보낸 사용자의 에러 큐로 알림이 가는지 검증한다.
 *
 * <p>동기 발행 때는 XADD 예외가 핸들러에서 바로 던져져 같은 경로로 알림이 갔다. 비동기로 바꾼 뒤에도 그 동작이
 * 유지되는지가 핵심이다. Redis 장애를 직접 만들 수 없으므로 발행 결과만 실패한 future로 바꿔 끼운다.</p>
 */
class AsyncPublishFailureNotificationTest extends RoomModuleWebSocketTest {

    private static final String JOIN_CODE = "ABCD";
    private static final String PLAYER = "다정한코끼리";
    private static final String USER_ERROR_QUEUE = "/user/queue/errors";
    private static final String ERROR_MESSAGE = "처리 중 오류가 발생했습니다.";

    @MockitoSpyBean
    private StreamPublisher streamPublisher;

    @Test
    void 비동기_발행이_실패하면_보낸_사용자에게_에러를_보낸다() throws Exception {
        // given — XADD가 나중에 실패한다
        doReturn(CompletableFuture.failedFuture(new IllegalStateException("Redis 연결 끊김")))
                .when(streamPublisher)
                .publishAsync(any(), any());

        try (final TestStompSession session = createSession(JOIN_CODE, PLAYER)) {
            final MessageCollector errors = session.subscribe(USER_ERROR_QUEUE);

            // when
            final MessageResponse received = sendUntilErrorArrives(session, errors);

            // then
            assertThat(received.payload()).contains("\"success\":false", ERROR_MESSAGE);
        }
    }

    /**
     * 개인 큐 구독은 등록 완료를 확인할 방법이 없어, 등록 전에 보낸 요청의 알림은 사라질 수 있다.
     * 그래서 알림이 도착할 때까지 요청을 다시 보낸다(TestStompSession의 구독 barrier와 같은 방식).
     * 방이 없어 생기는 다른 에러 알림은 건너뛴다.
     */
    private MessageResponse sendUntilErrorArrives(TestStompSession session, MessageCollector errors) {
        final AtomicReference<MessageResponse> found = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> {
                    session.send(
                            "/app/room/" + JOIN_CODE + "/update-ready",
                            new ReadyChangeMessage(JOIN_CODE, PLAYER, true));
                    findMessageContaining(errors, ERROR_MESSAGE).ifPresent(found::set);
                    return found.get() != null;
                });
        return found.get();
    }

    private Optional<MessageResponse> findMessageContaining(MessageCollector collector, String text) {
        while (!collector.isEmpty()) {
            final MessageResponse message = collector.get();
            if (message.payload().contains(text)) {
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }
}
