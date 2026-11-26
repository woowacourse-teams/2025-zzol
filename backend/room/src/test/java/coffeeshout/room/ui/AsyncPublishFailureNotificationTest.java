package coffeeshout.room.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import coffeeshout.RoomModuleWebSocketTest;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.ui.request.ReadyChangeMessage;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.TestStompSession.MessageCollector;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

/**
 * WebSocket 핸들러가 {@link StreamPublisher#publishAsync}의 future를 반환하면,
 * XADD가 나중에 실패해도 {@code @MessageExceptionHandler}를 거쳐 보낸 사용자의 에러 큐로 알림이 가는지 검증한다.
 *
 * <p>동기 발행 때는 XADD 예외가 핸들러에서 바로 던져져 같은 경로로 알림이 갔다. 비동기로 바꾼 뒤에는 실패가
 * 핸들러가 끝난 뒤 다른 스레드에서 나고, 알림도 Inbound 스레드가 아닌 스레드에서 보낸다. 그 경로를 그대로 지나도록
 * {@code StreamPublisher}는 실제 빈을 쓰고, Redis 명령만 조금 늦게 실패하는 가짜로 바꾼다.</p>
 */
class AsyncPublishFailureNotificationTest extends RoomModuleWebSocketTest {

    private static final String JOIN_CODE = "ABCD";
    private static final String PLAYER = "다정한코끼리";
    private static final String USER_ERROR_QUEUE = "/user/queue/errors";
    private static final String ERROR_MESSAGE = "처리 중 오류가 발생했습니다.";

    @MockitoBean
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Test
    void 비동기_발행이_나중에_다른_스레드에서_실패해도_보낸_사용자에게_에러를_보낸다() throws Exception {
        // given — XADD가 핸들러가 끝난 뒤 다른 스레드에서 실패한다
        givenXAddFailsLater();

        try (final TestStompSession session = createSession(JOIN_CODE, PLAYER)) {
            final MessageCollector errors = session.subscribe(USER_ERROR_QUEUE);

            // when
            final MessageResponse received = sendUntilErrorArrives(session, errors);

            // then
            assertThat(received.payload()).contains("\"success\":false", ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private void givenXAddFailsLater() {
        final ReactiveStreamOperations<String, Object, Object> streamOperations = mock(ReactiveStreamOperations.class);
        doReturn(streamOperations).when(reactiveStringRedisTemplate).opsForStream();
        given(streamOperations.add(any(MapRecord.class), any(XAddOptions.class)))
                .willReturn(Mono.<RecordId>error(new RedisConnectionFailureException("Redis 연결 끊김"))
                        .delaySubscription(Duration.ofMillis(50)));
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
