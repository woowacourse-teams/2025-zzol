package coffeeshout.settlement.application;

import coffeeshout.settlement.event.SeasonRankUpdatedEvent;
import coffeeshout.settlement.ui.response.SeasonRankMessage;
import coffeeshout.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.websocket.docs.WsTopic;
import coffeeshout.websocket.ui.WebSocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 순위 변동을 방의 WebSocket 토픽으로 뿌린다. 브로드캐스트 스트림을 소비한 각 인스턴스가
 * 자기 로컬 세션에만 전송하는 기존 알림 경로 그대로다(#1610).
 */
@Component
@RequiredArgsConstructor
public class SettlementNotifier {

    public static final String SETTLEMENT_DESTINATION_FORMAT = "/topic/room/%s/settlement";

    private final LoggingSimpMessagingTemplate messagingTemplate;

    @WsTopic(path = "/room/{joinCode}/settlement", payload = SeasonRankMessage.class,
            description = "미니게임 시즌 정산 완료 시 순위 변동 브로드캐스트")
    public void notifyRankUpdated(SeasonRankUpdatedEvent event) {
        messagingTemplate.convertAndSend(
                String.format(SETTLEMENT_DESTINATION_FORMAT, event.joinCode()),
                WebSocketResponse.success(SeasonRankMessage.from(event))
        );
    }
}
