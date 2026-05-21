package coffeeshout.blockstacking.application;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.minigame.domain.Gamer;
import coffeeshout.room.domain.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockStackingService implements MiniGameService {

    private final RoomQueryService roomQueryService;
    private final GameSessionRepository gameSessionRepository;
    private final BlockStackingFlowOrchestrator flowOrchestrator;
    private final BlockStackingNotifier notifier;


    @Override
    public void start(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final BlockStackingGame game = getGame(joinCode);
        flowOrchestrator.startFlow(game, room);
    }

    public void recordProgress(
            String joinCode, String playerName, Long userId, int floor,
            double movingBlockX, double stackTopX, double stackTopWidth
    ) {
        log.debug("블록 쌓기 진행 처리 시작: joinCode={}, playerName={}, floor={}",
                joinCode, playerName, floor);

        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final BlockStackingGame game = getGame(joinCode);

        final Gamer gamer = Gamer.of(playerName, userId);
        final boolean updated = game.recordProgress(gamer, floor, movingBlockX, stackTopX, stackTopWidth);
        if (updated) {
            notifier.notifyProgressUpdated(game, room);
        }
    }

    public void recordFailure(String joinCode, String playerName, Long userId) {
        log.debug("블록 쌓기 실패 처리 시작: joinCode={}, playerName={}", joinCode, playerName);

        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final BlockStackingGame game = getGame(joinCode);

        final Gamer gamer = Gamer.of(playerName, userId);
        final boolean recorded = game.recordFailure(gamer);
        if (recorded) {
            flowOrchestrator.triggerEarlyFinishIfAllFailed(joinCode, game);
        }
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.BLOCK_STACKING;
    }

    private BlockStackingGame getGame(String joinCode) {
        return (BlockStackingGame) gameSessionRepository.getByJoinCode(new JoinCode(joinCode))
                .findCompletedGame(MiniGameType.BLOCK_STACKING);
    }

}
