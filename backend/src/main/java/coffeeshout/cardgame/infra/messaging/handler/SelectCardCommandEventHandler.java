package coffeeshout.cardgame.infra.messaging.handler;

import coffeeshout.cardgame.application.CardGameService;
import coffeeshout.cardgame.domain.event.SelectCardCommandEvent;
import coffeeshout.minigame.event.MiniGameEventType;
import coffeeshout.minigame.infra.messaging.handler.MiniGameEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelectCardCommandEventHandler implements MiniGameEventHandler<SelectCardCommandEvent> {

    private final CardGameService cardGameService;

    @Override
    public void handle(SelectCardCommandEvent event) {
        try {
            log.info("카드 선택 이벤트 수신: eventId={}, joinCode={}, playerName={}, cardIndex={}",
                    event.eventId(), event.joinCode(), event.playerName(), event.cardIndex());

            cardGameService.selectCard(event.joinCode(), event.playerName(), event.cardIndex());

            log.info("카드 선택 이벤트 처리 완료: eventId={}, joinCode={}",
                    event.eventId(), event.joinCode());

        } catch (Exception e) {
            log.error("카드 선택 이벤트 처리 실패: eventId={}, joinCode={}, playerName={}, cardIndex={}",
                    event.eventId(), event.joinCode(), event.playerName(), event.cardIndex(), e);
        }
    }

    @Override
    public MiniGameEventType getSupportedEventType() {
        return MiniGameEventType.SELECT_CARD_COMMAND;
    }
}
