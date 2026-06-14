package coffeeshout.blockstacking.application;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockStackingService implements MiniGameService {

    private final GameSessionService gameSessionService;
    private final BlockStackingFlowOrchestrator flowOrchestrator;
    private final BlockStackingNotifier notifier;


    @Override
    public void start(String joinCode, String hostName) {
        final JoinCode code = new JoinCode(joinCode);
        final BlockStackingGame game = getGame(code);
        flowOrchestrator.startFlow(game, code);
    }

    public void recordProgress(
            String joinCode, String playerName, int floor,
            double movingBlockX, double stackTopX, double stackTopWidth
    ) {
        log.debug("블록 쌓기 진행 처리 시작: joinCode={}, playerName={}, floor={}",
                joinCode, playerName, floor);

        final JoinCode code = new JoinCode(joinCode);
        final BlockStackingGame game = getGame(code);
        final Gamer gamer = findGamer(game, playerName);

        final boolean updated = game.recordProgress(gamer, floor, movingBlockX, stackTopX, stackTopWidth);
        if (updated) {
            notifier.notifyProgressUpdated(game, code);
        }
    }

    public void recordFailure(String joinCode, String playerName) {
        log.debug("블록 쌓기 실패 처리 시작: joinCode={}, playerName={}", joinCode, playerName);

        final JoinCode code = new JoinCode(joinCode);
        final BlockStackingGame game = getGame(code);
        final Gamer gamer = findGamer(game, playerName);

        final boolean recorded = game.recordFailure(gamer);
        if (recorded) {
            flowOrchestrator.triggerEarlyFinishIfAllFailed(joinCode, game);
        }
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.BLOCK_STACKING;
    }

    private BlockStackingGame getGame(JoinCode joinCode) {
        return (BlockStackingGame) gameSessionService.getSession(joinCode)
                .findCompletedGame(MiniGameType.BLOCK_STACKING);
    }

    private Gamer findGamer(BlockStackingGame game, String playerName) {
        return game.findByName(playerName);
    }
}
