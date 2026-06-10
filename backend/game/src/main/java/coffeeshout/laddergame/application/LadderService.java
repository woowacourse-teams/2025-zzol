package coffeeshout.laddergame.application;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.laddergame.application.LadderCommandService;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LadderService implements MiniGameService {

    private final GameSessionService gameSessionService;
    private final LadderFlowOrchestrator flowOrchestrator;
    private final LadderCommandService commandService;
    private final LadderNotifier notifier;

    @Override
    public void start(String joinCode, String hostName) {
        final JoinCode code = new JoinCode(joinCode);
        final LadderGame game = getGame(code);
        flowOrchestrator.startFlow(game, code);
    }

    public void drawLine(String joinCode, String playerName, int segmentIndex) {
        log.debug("사다리게임 선 그리기 처리 시작: joinCode={}, playerName={}, segmentIndex={}",
                joinCode, playerName, segmentIndex);

        final JoinCode code = new JoinCode(joinCode);
        final LadderGame game = getGame(code);
        commandService.drawLine(game, playerName, segmentIndex)
                .ifPresent(line -> notifier.notifyLineDrawn(game, line, code));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.LADDER_GAME;
    }

    private LadderGame getGame(JoinCode joinCode) {
        return (LadderGame) gameSessionService.getSession(joinCode)
                .findCompletedGame(MiniGameType.LADDER_GAME);
    }
}
