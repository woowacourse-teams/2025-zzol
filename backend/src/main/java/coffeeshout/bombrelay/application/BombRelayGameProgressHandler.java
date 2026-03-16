package coffeeshout.bombrelay.application;

import coffeeshout.bombrelay.domain.BombRelayGame;
import coffeeshout.bombrelay.domain.WordValidationResult;
import coffeeshout.bombrelay.domain.event.BombRelayProgressEvent;
import coffeeshout.bombrelay.domain.event.WordResultEvent;
import coffeeshout.bombrelay.infra.WordValidator;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BombRelayGameProgressHandler {

    private final RoomQueryService roomQueryService;
    private final BombRelayGameService bombRelayGameService;
    private final ApplicationEventPublisher eventPublisher;
    private final WordValidator wordValidator;

    public void handleWord(String joinCode, String playerName, String word) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final BombRelayGame game = bombRelayGameService.getBombRelayGame(room);

        final WordValidationResult result = game.validateWord(playerName, word);

        if (result.isRejected()) {
            eventPublisher.publishEvent(
                    WordResultEvent.rejected(joinCode, playerName, word, result.errorCode().getMessage())
            );
            log.debug("단어 거절 (로컬 검증): joinCode={}, player={}, word={}, reason={}",
                    joinCode, playerName, word, result.errorCode());
            return;
        }

        if (result.requiresDictionaryCheck()) {
            if (!wordValidator.isValidWord(word)) {
                eventPublisher.publishEvent(
                        WordResultEvent.rejected(joinCode, playerName, word, "사전에 존재하지 않는 단어입니다.")
                );
                log.debug("단어 거절 (사전 검증): joinCode={}, player={}, word={}", joinCode, playerName, word);
                return;
            }
        }

        game.acceptWord(word);
        eventPublisher.publishEvent(WordResultEvent.accepted(joinCode, playerName, word));
        eventPublisher.publishEvent(BombRelayProgressEvent.of(game, joinCode));
        log.debug("단어 수락: joinCode={}, player={}, word={}", joinCode, playerName, word);
    }
}
