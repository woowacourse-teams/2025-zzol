package coffeeshout.racinggame.infra.messaging.handler;

import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.global.redis.EventHandler;
import coffeeshout.racinggame.application.RacingGameService;
import coffeeshout.racinggame.domain.event.TapCommandEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TapCommandEventHandler implements EventHandler<TapCommandEvent> {

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
    public Class<TapCommandEvent> eventType() {
        return TapCommandEvent.class;
    }
}
