package coffeeshout.minigame.infra.messaging.handler;

import coffeeshout.cardgame.domain.event.dto.MiniGameStartedEvent;
import coffeeshout.minigame.application.MiniGamePersistenceService;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.MiniGameEventType;
import coffeeshout.minigame.event.StartMiniGameCommandEvent;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.service.RoomQueryService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartMiniGameCommandEventHandler implements MiniGameEventHandler<StartMiniGameCommandEvent> {

    private final Map<MiniGameType, MiniGameService> miniGameServiceMap;
    private final RoomQueryService roomQueryService;
    private final ApplicationEventPublisher eventPublisher;
    private final MiniGamePersistenceService miniGamePersistenceService;

    public StartMiniGameCommandEventHandler(
            RoomQueryService roomQueryService,
            List<MiniGameService> miniGameServices,
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

    @Override
    public void handle(StartMiniGameCommandEvent event) {
        try {
            log.info("미니게임 시작 이벤트 수신: eventId={}, joinCode={}, hostName={}",
                    event.eventId(), event.joinCode(), event.hostName());

            updateRoomStateAndStartGame(event);

        } catch (Exception e) {
            log.error("미니게임 시작 이벤트 처리 실패: eventId={}, joinCode={}",
                    event.eventId(), event.joinCode(), e);
        }
    }

    private void updateRoomStateAndStartGame(StartMiniGameCommandEvent event) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(event.joinCode()));
        final Playable playable = room.startNextGame(event.hostName());
        eventPublisher.publishEvent(new MiniGameStartedEvent(event.joinCode(), playable.getMiniGameType().name()));
        miniGameServiceMap.get(playable.getMiniGameType()).start(event.joinCode(), event.hostName());
        miniGamePersistenceService.saveGameEntities(event, playable.getMiniGameType());
        log.info("JoinCode[{}] 미니게임 시작됨 - MiniGameType : {}", event.joinCode(), playable.getMiniGameType());
    }

    @Override
    public MiniGameEventType getSupportedEventType() {
        return MiniGameEventType.START_MINIGAME_COMMAND;
    }
}
