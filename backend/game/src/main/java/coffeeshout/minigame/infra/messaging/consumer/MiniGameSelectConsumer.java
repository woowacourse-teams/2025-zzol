package coffeeshout.minigame.infra.messaging.consumer;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.event.dto.MiniGameSelectEvent;
import coffeeshout.minigame.event.dto.MiniGameSelectFailedEvent;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 미니게임 선택 변경을 GameSession에 반영한다(ADR-0025 결정 4 — Option B).
 *
 * <p>호스트 검증은 GameSession이 단독 수행한다. 세션은 방 생성 시 {@code GameSessionInitConsumer}가
 * {@code GameRoomCreatedEvent}의 권위 있는 hostName으로 사전 생성하므로, {@code updateGames} 내부의
 * {@code replaceGames} 호스트 검증이 "select가 주장한 hostName == 권위 있는 호스트 이름"을 보증한다.
 * 따라서 더 이상 {@code RoomQueryService}로 방을 조회해 검증하지 않는다(:game → :room 의존 제거).
 * 반영 성공 후 in-process 이벤트를 재발행해 선택 목록 브로드캐스트({@code RoomMessagePublisher.onMiniGameListChanged})를
 * 트리거한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameSelectConsumer implements Consumer<MiniGameSelectEvent> {

    private final GameSessionService gameSessionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void accept(MiniGameSelectEvent event) {
        try {
            gameSessionService.updateGames(event);
        } catch (BusinessException e) {
            // 비동기 경로라 EventDispatcher가 예외를 삼켜 클라이언트가 거부 사실을 알 수 없으므로,
            // 실패 이벤트를 발행해 :room 리스너가 요청 클라이언트에게만 에러를 되돌리게 한다(ADR-0025).
            log.warn("미니게임 선택 반영 실패: joinCode={}, principal={}, errorCode={}, message={}",
                    event.joinCode(), event.principalName(), e.getErrorCode().getCode(), e.getMessage());
            eventPublisher.publishEvent(new MiniGameSelectFailedEvent(
                    event.joinCode(), event.principalName(), e.getErrorCode().getMessage()));
            return;
        }
        eventPublisher.publishEvent(event);
    }
}
