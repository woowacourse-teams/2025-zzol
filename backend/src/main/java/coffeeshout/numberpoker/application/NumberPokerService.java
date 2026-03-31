package coffeeshout.numberpoker.application;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.numberpoker.domain.NumberPokerErrorCode;
import coffeeshout.numberpoker.domain.NumberPokerGame;
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
public class NumberPokerService implements MiniGameService {

    private final RoomQueryService roomQueryService;
    private final NumberPokerGameStore gameStore;
    private final NumberPokerFlowOrchestrator flowOrchestrator;
    private final NumberPokerNotifier notifier;

    @Override
    public void start(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final NumberPokerGame game = (NumberPokerGame) room.findMiniGame(MiniGameType.NUMBER_POKER);
        gameStore.save(joinCode, game);
        flowOrchestrator.startFlow(game, room);
        log.info("넘버포커 게임 시작: joinCode={}", joinCode);
    }

    @Override
    public MiniGameType getMiniGameType() {
        return MiniGameType.NUMBER_POKER;
    }

    public void fold(String joinCode, String playerName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final NumberPokerGame game = gameStore.get(joinCode);
        final Player player = room.findPlayer(new PlayerName(playerName));
        game.fold(player);
        notifier.notifyPhaseChanged(game, room);
        log.debug("폴드: joinCode={}, player={}", joinCode, playerName);
    }

    public void ready(String joinCode, String playerName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final NumberPokerGame game = gameStore.get(joinCode);
        final Player player = room.findPlayer(new PlayerName(playerName));
        game.markReady(player);
        notifier.notifyPhaseChanged(game, room);
        if (game.isAllReady()) {
            flowOrchestrator.triggerEarlyRoundReady(joinCode);
        }
        log.debug("레디: joinCode={}, player={}", joinCode, playerName);
    }

    public void configureRoundCount(String joinCode, String hostName, int roundCount) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        validateHost(room, hostName);
        final NumberPokerGame game = gameStore.get(joinCode);
        game.configureRoundCount(roundCount);
        log.debug("라운드 수 설정: joinCode={}, roundCount={}", joinCode, roundCount);
    }

    private void validateHost(Room room, String hostName) {
        final Player player = room.findPlayer(new PlayerName(hostName));
        if (!room.isHost(player)) {
            throw new BusinessException(
                    NumberPokerErrorCode.NOT_HOST,
                    "호스트만 설정을 변경할 수 있습니다. player=" + hostName
            );
        }
    }
}
