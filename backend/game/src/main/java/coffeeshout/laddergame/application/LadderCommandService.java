package coffeeshout.laddergame.application;

import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.laddergame.domain.LadderGameState;
import coffeeshout.laddergame.domain.LadderLine;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Optional;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LadderCommandService {

    public Optional<LadderLine> drawLine(LadderGame game, String playerName, int segmentIndex) {
        final PlayerName name = new PlayerName(playerName);

        if (game.getState() != LadderGameState.DRAWING) {
            log.warn("DRAWING 상태가 아닐 때 선 그리기 요청 — 무시: playerName={}, state={}",
                    playerName, game.getState());
            return Optional.empty();
        }

        if (!game.getPoles().contains(name)) {
            log.warn("미참여자 선 그리기 요청 — 무시: playerName={}", playerName);
            return Optional.empty();
        }

        if (game.isAlreadyDrew(name)) {
            log.warn("이미 선을 그은 플레이어 재요청 — 무시: playerName={}", playerName);
            return Optional.empty();
        }

        if (!game.getPoles().isValidSegment(segmentIndex)) {
            log.warn("유효하지 않은 segmentIndex — 무시: playerName={}, segmentIndex={}",
                    playerName, segmentIndex);
            return Optional.empty();
        }

        return Optional.of(game.drawLine(name, segmentIndex));
    }
}
