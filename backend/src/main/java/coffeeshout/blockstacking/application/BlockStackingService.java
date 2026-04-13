package coffeeshout.blockstacking.application;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockStackingService implements MiniGameService {

    private final RoomQueryService roomQueryService;
    private final BlockStackingFlowOrchestrator flowOrchestrator;
    private final BlockStackingNotifier notifier;


    @Override
    public void start(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final BlockStackingGame game = getGame(room);
        flowOrchestrator.startFlow(game, room);
    }

    public void recordProgress(
            String joinCode, String playerName, int floor,
            double movingBlockX, double stackTopX, double stackTopWidth
    ) {
        log.debug("블록 쌓기 진행 처리 시작: joinCode={}, playerName={}, floor={}",
                joinCode, playerName, floor);

        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final BlockStackingGame game = (BlockStackingGame) room.findMiniGame(MiniGameType.BLOCK_STACKING);
        final Player player = game.findPlayerByName(new PlayerName(playerName));

        final boolean updated = game.recordProgress(player, floor, movingBlockX, stackTopX, stackTopWidth);
        if (updated) {
            notifier.notifyProgressUpdated(game, room);
        }
    }

    public void recordFailure(String joinCode, String playerName) {
        log.debug("블록 쌓기 실패 처리 시작: joinCode={}, playerName={}", joinCode, playerName);

        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final BlockStackingGame game = getGame(room);
        final Player player = game.findPlayerByName(new PlayerName(playerName));

        final boolean recorded = game.recordFailure(player);
        if (recorded) {
            flowOrchestrator.triggerEarlyFinishIfAllFailed(joinCode, game);
        }
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.BLOCK_STACKING;
    }

    private BlockStackingGame getGame(Room room) {
        return (BlockStackingGame) room.findMiniGame(MiniGameType.BLOCK_STACKING);
    }
}
