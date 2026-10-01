package coffeeshout.laddergame.application;

import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.laddergame.domain.LadderGameState;
import coffeeshout.laddergame.domain.LadderLine;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LadderCommandService {

    public Optional<LadderLine> drawLine(LadderGame game, String playerName, int segmentIndex, int row) {
        if (game.getState() != LadderGameState.DRAWING) {
            log.warn("DRAWING 상태가 아닐 때 선 그리기 요청 — 무시: playerName={}, state={}", playerName, game.getState());
            return Optional.empty();
        }

        if (!game.getPoles().contains(playerName)) {
            log.warn("미참여자 선 그리기 요청 — 무시: playerName={}", playerName);
            return Optional.empty();
        }

        if (!game.canDraw(playerName)) {
            log.warn("선을 모두 그은 플레이어 재요청 — 무시: playerName={}", playerName);
            return Optional.empty();
        }

        if (!game.getPoles().isValidSegment(segmentIndex)) {
            log.warn("유효하지 않은 segmentIndex — 무시: playerName={}, segmentIndex={}", playerName, segmentIndex);
            return Optional.empty();
        }

        if (!game.isValidRow(row)) {
            log.warn("유효하지 않은 row — 무시: playerName={}, row={}", playerName, row);
            return Optional.empty();
        }

        // 여기서 비어 있어도 추가 직전에 다른 요청이 먼저 차지할 수 있다. 그 경합은 LadderLines.add가 막고 Consumer가 무시한다.
        if (game.isOccupied(segmentIndex, row)) {
            log.warn("이미 선이 있는 자리 — 무시: playerName={}, segmentIndex={}, row={}", playerName, segmentIndex, row);
            return Optional.empty();
        }

        return Optional.of(game.drawLine(playerName, segmentIndex, row));
    }
}
