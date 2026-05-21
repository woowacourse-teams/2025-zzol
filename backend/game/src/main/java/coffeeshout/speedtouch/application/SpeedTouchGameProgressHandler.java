package coffeeshout.speedtouch.application;

import coffeeshout.minigame.domain.Gamer;
import coffeeshout.speedtouch.domain.SpeedTouchGame;
import coffeeshout.speedtouch.domain.event.SpeedTouchProgressEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpeedTouchGameProgressHandler {

    private final SpeedTouchGameService speedTouchGameService;
    private final ApplicationEventPublisher eventPublisher;

    public void handleTouch(String joinCode, String playerName, Long userId, int touchedNumber) {
        final SpeedTouchGame game = speedTouchGameService.getSpeedTouchGame(joinCode);

        final Gamer gamer = Gamer.of(playerName, userId);
        final boolean accepted = game.touch(gamer, touchedNumber, Instant.now());
        if (!accepted) {
            log.debug("터치 무시: joinCode={}, player={}, number={}", joinCode, playerName, touchedNumber);
            return;
        }

        eventPublisher.publishEvent(SpeedTouchProgressEvent.of(game, joinCode));
        log.debug("터치 처리: joinCode={}, player={}, number={}", joinCode, playerName, touchedNumber);

        if (game.isAllFinished()) {
            log.info("전원 완주 - 게임 종료: joinCode={}", joinCode);
            speedTouchGameService.finishGame(game, joinCode);
        }
    }
}
