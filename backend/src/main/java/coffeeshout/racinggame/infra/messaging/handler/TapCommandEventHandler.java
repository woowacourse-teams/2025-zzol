package coffeeshout.racinggame.infra.messaging.handler;

import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.racinggame.application.RacingGameService;
import coffeeshout.racinggame.domain.event.RacingGameEventType;
import coffeeshout.racinggame.domain.event.TapCommandEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Redis 이벤트를 수신하여 Command Service로 라우팅하는 핸들러
 * 메시지 수신과 로깅만 담당하며, 비즈니스 로직은 CommandService에 위임합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TapCommandEventHandler implements RacingGameEventHandler<TapCommandEvent> {

    private final RacingGameService racingGameService;

    @Override
    public void handle(TapCommandEvent event) {
        try {
//            log.debug("탭 이벤트 수신: eventId={}, joinCode={}, playerName={}, tapCount={}",
//                    event.eventId(), event.joinCode(), event.playerName(), event.tapCount());

            racingGameService.tap(
                    event.joinCode(),
                    event.playerName(),
                    event.tapCount()
            );

        } catch (InvalidStateException e) {
            log.warn("탭 이벤트 처리 중 상태 오류: eventId={}, joinCode={}",
                    event.eventId(), event.joinCode(), e);
        } catch (Exception e) {
            log.error("탭 이벤트 처리 실패: eventId={}, joinCode={}",
                    event.eventId(), event.joinCode(), e);
        }
    }

    @Override
    public RacingGameEventType getSupportedEventType() {
        return RacingGameEventType.TAP_COMMAND;
    }
}
