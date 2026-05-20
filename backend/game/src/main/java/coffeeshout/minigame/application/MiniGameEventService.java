package coffeeshout.minigame.application;

import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.GameSession;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.Playable;
import coffeeshout.minigame.event.MiniGameStartedEvent;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.domain.service.RoomQueryService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MiniGameEventService {

    private final Map<MiniGameType, MiniGameService> miniGameServiceMap;
    private final RoomQueryService roomQueryService;
    private final RoomCommandService roomCommandService;
    private final ApplicationEventPublisher eventPublisher;
    private final MiniGamePersistenceService miniGamePersistenceService;
    private final GameSessionService gameSessionService;

    public MiniGameEventService(
            RoomQueryService roomQueryService,
            RoomCommandService roomCommandService,
            List<MiniGameService> miniGameServices,
            ApplicationEventPublisher eventPublisher,
            MiniGamePersistenceService miniGamePersistenceService,
            GameSessionService gameSessionService
    ) {
        this.roomQueryService = roomQueryService;
        this.roomCommandService = roomCommandService;
        this.eventPublisher = eventPublisher;
        this.miniGamePersistenceService = miniGamePersistenceService;
        this.gameSessionService = gameSessionService;
        this.miniGameServiceMap = new EnumMap<>(MiniGameType.class);
        miniGameServices.forEach(svc -> miniGameServiceMap.put(svc.getMiniGameType(), svc));
    }

    public void startMiniGame(StartMiniGameCommandEvent event) {
        log.info("미니게임 시작 이벤트 수신: eventId={}, joinCode={}, hostName={}",
                event.eventId(), event.joinCode(), event.hostName());

        final JoinCode joinCode = new JoinCode(event.joinCode());
        final PlayerName hostName = new PlayerName(event.hostName());

        final Room room = roomQueryService.getByJoinCode(joinCode);
        final GameSession session = gameSessionService.getOrCreateSession(joinCode);

        final Playable playable = session.startNextGame(hostName, room.getPlayers());
        final MiniGameType miniGameType = playable.getMiniGameType();

        roomCommandService.markPlaying(joinCode, hostName);

        final MiniGameService miniGameService = Optional.ofNullable(miniGameServiceMap.get(miniGameType))
                .orElseThrow(() -> new IllegalStateException("미니게임 서비스가 등록되지 않았습니다: " + miniGameType));
        miniGameService.start(event.joinCode(), event.hostName());

        miniGamePersistenceService.saveGameEntities(event, miniGameType, session);

        eventPublisher.publishEvent(new MiniGameStartedEvent(event.joinCode(), miniGameType.name()));
        log.info("JoinCode[{}] 미니게임 시작됨 - MiniGameType : {}", event.joinCode(), miniGameType);
    }
}
