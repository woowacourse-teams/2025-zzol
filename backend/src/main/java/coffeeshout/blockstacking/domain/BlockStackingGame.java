package coffeeshout.blockstacking.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class BlockStackingGame implements Playable {

    private BlockStackingGameState state;
    private Map<Player, BlockStackingPlayerProgress> playerProgresses;

    public BlockStackingGame() {
        this.state = BlockStackingGameState.READY;
        this.playerProgresses = new HashMap<>();
    }

    @Override
    public void setUp(List<Player> players) {
        this.playerProgresses = players.stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> BlockStackingPlayerProgress.initial(p.getName())
                ));
    }

    public void prepare() {
        this.state = BlockStackingGameState.PREPARE;
    }

    public void startPlay() {
        this.state = BlockStackingGameState.PLAYING;
    }

    public void finish() {
        this.state = BlockStackingGameState.DONE;
    }

    /**
     * 플레이어의 블록 안착 이벤트를 기록한다.
     *
     * @return 유효한 이벤트면 true, 검증 실패로 무시됐으면 false
     */
    public boolean recordProgress(
            Player player, int floor,
            double tapX, double movingBlockX,
            double stackTopX, double stackTopWidth
    ) {
        if (state != BlockStackingGameState.PLAYING) {
            throw new BusinessException(
                    BlockStackingGameErrorCode.NOT_PLAYING_STATE,
                    "현재 게임이 진행중인 상태가 아닙니다. state=" + state
            );
        }

        final BlockStackingPlayerProgress progress = playerProgresses.get(player);
        if (!isValidFloorSequence(floor, progress.currentFloor())) {
            log.warn("[{}] 비연속적 층수 수신 — 무시: player={}, expected={}, received={}",
                    BlockStackingGameErrorCode.INVALID_PROGRESS.getCode(),
                    player.getName().value(), progress.currentFloor() + 1, floor);
            return false;
        }
        final double overlap = calculateOverlap(movingBlockX, stackTopX, stackTopWidth);
        if (overlap <= 0) {
            log.warn("[{}] 유효하지 않은 overlap — 무시: player={}, floor={}, overlap={}",
                    BlockStackingGameErrorCode.INVALID_PROGRESS.getCode(),
                    player.getName().value(), floor, overlap);
            return false;
        }
        playerProgresses.put(player, progress.advanceTo(floor));
        return true;
    }

    public List<BlockStackingPlayerRankInfo> getRanking() {
        return playerProgresses.values().stream()
                .sorted(Comparator.comparingInt(BlockStackingPlayerProgress::currentFloor).reversed())
                .map(p -> new BlockStackingPlayerRankInfo(p.playerName().value(), p.currentFloor()))
                .toList();
    }

    public Player findPlayerByName(PlayerName playerName) {
        return playerProgresses.keySet().stream()
                .filter(p -> p.sameName(playerName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        BlockStackingGameErrorCode.PLAYER_NOT_FOUND,
                        "플레이어를 찾을 수 없습니다: " + playerName.value()
                ));
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromDescending(getScores());
    }

    @Override
    public Map<Player, MiniGameScore> getScores() {
        return playerProgresses.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new BlockStackingScore(e.getValue().currentFloor())
                ));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.BLOCK_STACKING;
    }

    private boolean isValidFloorSequence(int floor, int currentFloor) {
        return floor == currentFloor + 1;
    }

    private double calculateOverlap(double movingBlockX, double stackTopX, double stackTopWidth) {
        return Math.min(movingBlockX + stackTopWidth, stackTopX + stackTopWidth)
                - Math.max(movingBlockX, stackTopX);
    }
}
