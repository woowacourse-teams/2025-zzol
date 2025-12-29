package coffeeshout.minigame.application;

import coffeeshout.cardgame.domain.event.dto.MiniGameStartedEvent;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.Room;
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

    private final Map<MiniGameType, coffeeshout.minigame.domain.MiniGameService> miniGameServiceMap;
    private final RoomQueryService roomQueryService;
    private final ApplicationEventPublisher eventPublisher;
    private final MiniGamePersistenceService miniGamePersistenceService;

    public MiniGameEventService(
            RoomQueryService roomQueryService,
            List<coffeeshout.minigame.domain.MiniGameService> miniGameServices,
            ApplicationEventPublisher eventPublisher,
            MiniGamePersistenceService miniGamePersistenceService
    ) {
        this.roomQueryService = roomQueryService;
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

        final Room room = roomQueryService.getByJoinCode(new JoinCode(event.joinCode()));
        final Playable playable = room.startNextGame(event.hostName());
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
