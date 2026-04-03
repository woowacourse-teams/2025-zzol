package coffeeshout.blockstacking.domain.service;

import coffeeshout.blockstacking.application.BlockStackingNotifier;
import coffeeshout.blockstacking.domain.BlockStackingGame;
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
public class BlockStackingCommandService {

    private final RoomQueryService roomQueryService;
    private final BlockStackingNotifier notifier;

    public void recordProgress(
            JoinCode joinCode, PlayerName playerName, int floor,
            double movingBlockX, double stackTopX, double stackTopWidth
    ) {
        log.debug("블록 쌓기 진행 처리 시작: joinCode={}, playerName={}, floor={}",
                joinCode, playerName, floor);

        final Room room = roomQueryService.getByJoinCode(joinCode);
        final BlockStackingGame game = (BlockStackingGame) room.findMiniGame(MiniGameType.BLOCK_STACKING);
        final Player player = game.findPlayerByName(playerName);

        game.recordProgress(player, floor, movingBlockX, stackTopX, stackTopWidth);
        notifier.notifyProgressUpdated(game, room);
    }
}
