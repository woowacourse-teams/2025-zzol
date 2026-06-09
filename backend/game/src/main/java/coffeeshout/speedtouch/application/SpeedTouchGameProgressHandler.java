package coffeeshout.speedtouch.application;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.application.service.RoomQueryService;
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

    private final RoomQueryService roomQueryService;
    private final SpeedTouchGameService speedTouchGameService;
    private final ApplicationEventPublisher eventPublisher;

    public void handleTouch(String joinCode, String playerName, int touchedNumber) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final SpeedTouchGame game = speedTouchGameService.getSpeedTouchGame(room);

        final boolean accepted = game.touch(new PlayerName(playerName), touchedNumber, Instant.now());
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
