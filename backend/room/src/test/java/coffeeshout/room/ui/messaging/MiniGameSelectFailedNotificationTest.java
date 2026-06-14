package coffeeshout.room.ui.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.RoomModuleWebSocketTest;
import coffeeshout.minigame.event.dto.MiniGameSelectFailedEvent;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.TestStompSession.MessageCollector;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * {@link RoomMessagePublisher#onMiniGameSelectFailed}가 발행한 실패 알림이 요청 클라이언트의
 * 개인 에러 큐({@code /user/queue/errors})로 전달되는지, 그리고 다른 클라이언트로는 새지 않는지 검증한다.
 *
 * <p>프로덕션에서는 Redis Stream Consumer 스레드가 {@code MiniGameSelectFailedEvent}를 발행한다.
 * 여기서는 테스트 스레드에서 직접 발행해 동일한 비-인바운드 스레드 전달 경로를 재현한다 —
 * "백엔드가 정말 전달하는가 / 대상에게만 전달하는가"를 결정적으로 가른다.
 */
class MiniGameSelectFailedNotificationTest extends RoomModuleWebSocketTest {

    private static final String JOIN_CODE = "ABCD";
    private static final String TARGET_PLAYER = "다정한코끼리";
    private static final String TARGET_PRINCIPAL = JOIN_CODE + ":" + TARGET_PLAYER;
    private static final String BYSTANDER_PLAYER = "구경하는다람쥐";
    private static final String USER_ERROR_QUEUE = "/user/queue/errors";
    private static final String ERROR_MESSAGE = "호스트만 미니게임을 선택할 수 있습니다.";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("실패 알림은 요청 클라이언트의 개인 에러 큐로만 전달되고 다른 클라이언트로는 새지 않는다")
    void 요청자에게만_전달된다() throws Exception {
        try (final TestStompSession target = createSession(JOIN_CODE, TARGET_PLAYER);
             final TestStompSession bystander = createSession(JOIN_CODE, BYSTANDER_PLAYER)) {
            assertThat(target.getPrincipalName()).isEqualTo(TARGET_PRINCIPAL);

            final MessageCollector targetCollector = target.subscribe(USER_ERROR_QUEUE);
            final MessageCollector bystanderCollector = bystander.subscribe(USER_ERROR_QUEUE);
            settleSubscriptions();
            // 방 미존재로 인한 연결 실패 알림 등 사전 메시지를 제거한 뒤 발행 효과만 관찰한다.
            drain(targetCollector);
            drain(bystanderCollector);

            eventPublisher.publishEvent(new MiniGameSelectFailedEvent(JOIN_CODE, TARGET_PRINCIPAL, ERROR_MESSAGE));

            final MessageResponse received = targetCollector.get();
            assertThat(received.payload()).contains("\"success\":false");
            assertThat(received.payload()).contains(ERROR_MESSAGE);

            bystanderCollector.assertNoMessage();
        }
    }

    private void settleSubscriptions() {
        // SUBSCRIBE 프레임이 브로커에 등록될 시간을 확보한다(전송과 비동기).
        Awaitility.await().pollDelay(Duration.ofMillis(500)).until(() -> true);
    }

    private void drain(MessageCollector collector) {
        while (!collector.isEmpty()) {
            collector.get();
        }
    }
}
