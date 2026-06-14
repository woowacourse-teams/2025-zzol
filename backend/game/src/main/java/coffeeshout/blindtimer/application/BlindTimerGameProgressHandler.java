package coffeeshout.blindtimer.application;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blindtimer.domain.event.BlindTimerProgressEvent;
import coffeeshout.gamecommon.JoinCode;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindTimerGameProgressHandler {

    private final BlindTimerGameService blindTimerGameService;
    private final ApplicationEventPublisher eventPublisher;

    public void handleStop(String joinCode, String playerName) {
        final BlindTimerGame game = blindTimerGameService.getBlindTimerGame(new JoinCode(joinCode));

        final boolean accepted = game.stop(playerName, Instant.now());
        if (!accepted) {
            log.debug("STOP 무시 (이미 멈춤): joinCode={}, player={}", joinCode, playerName);
            return;
        }

        eventPublisher.publishEvent(BlindTimerProgressEvent.of(game, joinCode));
        log.debug("STOP 처리: joinCode={}, player={}", joinCode, playerName);

        if (game.isAllStopped()) {
            log.info("전원 STOP - 게임 종료: joinCode={}", joinCode);
            blindTimerGameService.finishGame(game, joinCode);
        }
    }
}
