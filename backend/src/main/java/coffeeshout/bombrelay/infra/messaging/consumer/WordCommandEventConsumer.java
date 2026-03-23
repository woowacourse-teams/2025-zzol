package coffeeshout.bombrelay.infra.messaging.consumer;

import coffeeshout.bombrelay.application.BombRelayGameProgressHandler;
import coffeeshout.bombrelay.domain.event.WordCommandEvent;
import coffeeshout.global.exception.custom.InvalidStateException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WordCommandEventConsumer implements Consumer<WordCommandEvent> {

    private final BombRelayGameProgressHandler progressHandler;

    @Override
    public void accept(WordCommandEvent event) {
        try {
            progressHandler.handleWord(event.joinCode(), event.playerName(), event.word());
        } catch (InvalidStateException e) {
            log.warn("단어 입력 이벤트 처리 중 상태 오류: eventId={}, joinCode={}",
                    event.eventId(), event.joinCode(), e);
        } catch (Exception e) {
            log.error("단어 입력 이벤트 처리 실패: eventId={}, joinCode={}",
                    event.eventId(), event.joinCode(), e);
        }
    }
}
