package coffeeshout.blockstacking.domain;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class BlockStackingGame implements Playable {

    private volatile BlockStackingGameState state;
    private final ConcurrentHashMap<Gamer, BlockStackingPlayerProgress> playerProgresses = new ConcurrentHashMap<>();

    public BlockStackingGame() {
        this.state = BlockStackingGameState.READY;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
        playerProgresses.clear();
        gamers.forEach(gamer -> playerProgresses.put(gamer, BlockStackingPlayerProgress.initial(gamer)));
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
    public synchronized boolean recordProgress(
            Gamer gamer, int floor,
            double movingBlockX, double stackTopX, double stackTopWidth
    ) {
        if (state != BlockStackingGameState.PLAYING) {
            throw new BusinessException(
                    BlockStackingGameErrorCode.NOT_PLAYING_STATE,
                    "현재 게임이 진행중인 상태가 아닙니다. state=" + state
            );
        }

        final BlockStackingPlayerProgress progress = playerProgresses.get(gamer);
        if (progress == null) {
            log.warn("[{}] 등록되지 않은 플레이어의 진행 이벤트 수신 — 무시: player={}",
                    BlockStackingGameErrorCode.PLAYER_NOT_FOUND.getCode(),
                    gamer.getName());
            throw new BusinessException(
                    BlockStackingGameErrorCode.PLAYER_NOT_FOUND,
                    "등록되지 않은 플레이어입니다: " + gamer.getName()
            );
        }

        if (progress.failed()) {
            log.warn("[{}] 이미 실패한 플레이어의 진행 이벤트 수신 — 무시: player={}",
                    BlockStackingGameErrorCode.INVALID_PROGRESS.getCode(),
                    gamer.getName());
            return false;
        }

        if (!isValidFloorSequence(floor, progress.currentFloor())) {
            log.warn("[{}] 비연속적 층수 수신 — 무시: player={}, expected={}, received={}",
                    BlockStackingGameErrorCode.INVALID_PROGRESS.getCode(),
                    gamer.getName(), progress.currentFloor() + 1, floor);
            return false;
        }

        final double overlap = calculateOverlap(movingBlockX, stackTopX, stackTopWidth);
        if (overlap <= 0) {
            log.warn("[{}] 유효하지 않은 overlap — 무시: player={}, floor={}, overlap={}",
                    BlockStackingGameErrorCode.INVALID_PROGRESS.getCode(),
                    gamer.getName(), floor, overlap);
            return false;
        }

        playerProgresses.put(gamer, progress.advanceTo(floor));
        return true;
    }

    /**
     * 플레이어의 실패를 기록한다.
     *
     * @return 실패 상태로 전환됐으면 true, 이미 실패한 상태였으면 false
     * @throws BusinessException 게임이 PLAYING 상태가 아닐 때, 또는 등록되지 않은 플레이어일 때
     */
    public synchronized boolean recordFailure(Gamer gamer) {
        if (state != BlockStackingGameState.PLAYING) {
            throw new BusinessException(
                    BlockStackingGameErrorCode.NOT_PLAYING_STATE,
                    "현재 게임이 진행중인 상태가 아닙니다. state=" + state
            );
        }

        final BlockStackingPlayerProgress progress = playerProgresses.get(gamer);
        if (progress == null) {
            throw new BusinessException(
                    BlockStackingGameErrorCode.PLAYER_NOT_FOUND,
                    "등록되지 않은 플레이어입니다: " + gamer.getName()
            );
        }

        if (progress.failed()) {
            return false;
        }
        playerProgresses.put(gamer, progress.fail());
        log.info("플레이어 실패 기록: player={}, floor={}", gamer.getName(), progress.currentFloor());
        return true;
    }

    public boolean isAllPlayersFailed() {
        return !playerProgresses.isEmpty()
                && playerProgresses.values().stream().allMatch(BlockStackingPlayerProgress::failed);
    }

    public List<BlockStackingPlayerRankInfo> getRanking() {
        return playerProgresses.values().stream()
                .sorted(Comparator.comparingInt(BlockStackingPlayerProgress::currentFloor).reversed()
                        .thenComparing(p -> p.gamer().getName()))
                .map(p -> new BlockStackingPlayerRankInfo(p.gamer().getName(), p.currentFloor()))
                .toList();
    }

    public Gamer findByName(String name) {
        return playerProgresses.keySet().stream()
                .filter(gamer -> gamer.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        BlockStackingGameErrorCode.PLAYER_NOT_FOUND,
                        "플레이어를 찾을 수 없습니다: " + name
                ));
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromDescending(getScores());
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
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
