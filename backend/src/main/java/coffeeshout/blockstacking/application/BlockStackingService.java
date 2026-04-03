package coffeeshout.blockstacking.application;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.blockstacking.domain.service.BlockStackingCommandService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
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
    private final BlockStackingCommandService commandService;
    private final BlockStackingFlowOrchestrator flowOrchestrator;

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
        commandService.recordProgress(
                new JoinCode(joinCode), new PlayerName(playerName),
                floor, movingBlockX, stackTopX, stackTopWidth
        );
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.BLOCK_STACKING;
    }

    private BlockStackingGame getGame(Room room) {
        return (BlockStackingGame) room.findMiniGame(MiniGameType.BLOCK_STACKING);
    }
}
