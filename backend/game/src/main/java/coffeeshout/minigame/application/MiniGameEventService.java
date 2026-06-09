package coffeeshout.minigame.application;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.MiniGameStartedEvent;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.gamecommon.Playable;
import coffeeshout.room.domain.Room;
import coffeeshout.room.application.service.RoomQueryService;
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

    private final Map<MiniGameType, coffeeshout.minigame.domain.MiniGameService> miniGameServiceMap;
    private final RoomQueryService roomQueryService;
    private final GameSessionService gameSessionService;
    private final ApplicationEventPublisher eventPublisher;
    private final MiniGamePersistenceService miniGamePersistenceService;

    public MiniGameEventService(
            RoomQueryService roomQueryService,
            GameSessionService gameSessionService,
            List<coffeeshout.minigame.domain.MiniGameService> miniGameServices,
            ApplicationEventPublisher eventPublisher,
            MiniGamePersistenceService miniGamePersistenceService
    ) {
        this.roomQueryService = roomQueryService;
        this.gameSessionService = gameSessionService;
        this.eventPublisher = eventPublisher;
        this.miniGamePersistenceService = miniGamePersistenceService;
        this.miniGameServiceMap = new EnumMap<>(MiniGameType.class);
        miniGameServices.forEach(miniGameService -> miniGameServiceMap.put(
                miniGameService.getMiniGameType(),
                miniGameService
        ));
    }

    public void startMiniGame(StartMiniGameCommandEvent event) {
        log.info("미니게임 시작 이벤트 수신: eventId={}, joinCode={}, hostName={}",
                event.eventId(), event.joinCode(), event.hostName());

        final JoinCode joinCode = new JoinCode(event.joinCode());
        final Room room = roomQueryService.getByJoinCode(joinCode);

        // 검증(읽기 전용) → 대기열 전이(GameSession) → PLAYING 전이(Room) 순서로 분리 (ADR-0023 결정 4)
        room.validateStartable(event.hostName());
        final Playable playable = gameSessionService.startGame(
                joinCode, Gamer.guest(event.hostName()), room.getGamers());
        room.markPlaying();

        final MiniGameType miniGameType = playable.getMiniGameType();

        final MiniGameService miniGameService = Optional.ofNullable(miniGameServiceMap.get(miniGameType))
                .orElseThrow(() -> new IllegalStateException("미니게임 서비스가 등록되지 않았습니다: " + miniGameType));
        miniGameService.start(event.joinCode(), event.hostName());

        miniGamePersistenceService.saveGameEntities(event, miniGameType);

        // 모든 작업 성공 후 이벤트 발행
        eventPublisher.publishEvent(new MiniGameStartedEvent(event.joinCode(), miniGameType.name()));
        log.info("JoinCode[{}] 미니게임 시작됨 - MiniGameType : {}", event.joinCode(), miniGameType);
    }
}
