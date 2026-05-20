package coffeeshout.laddergame.application;

import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.laddergame.domain.service.LadderCommandService;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.service.RoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LadderService implements MiniGameService {

    private final RoomQueryService roomQueryService;
    private final GameSessionRepository gameSessionRepository;
    private final LadderFlowOrchestrator flowOrchestrator;
    private final LadderCommandService commandService;
    private final LadderNotifier notifier;

    @Override
    public void start(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final LadderGame game = getGame(joinCode);
        flowOrchestrator.startFlow(game, room);
    }

    public void drawLine(String joinCode, String playerName, int segmentIndex) {
        log.debug("사다리게임 선 그리기 처리 시작: joinCode={}, playerName={}, segmentIndex={}",
                joinCode, playerName, segmentIndex);

        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final LadderGame game = getGame(joinCode);
        commandService.drawLine(game, playerName, segmentIndex)
                .ifPresent(line -> notifier.notifyLineDrawn(line, room.getJoinCode(), room.toColorIndexMap()));
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.LADDER_GAME;
    }

    private LadderGame getGame(String joinCode) {
        return (LadderGame) gameSessionRepository.getByJoinCode(new JoinCode(joinCode))
                .findCompletedGame(MiniGameType.LADDER_GAME);
    }
}
