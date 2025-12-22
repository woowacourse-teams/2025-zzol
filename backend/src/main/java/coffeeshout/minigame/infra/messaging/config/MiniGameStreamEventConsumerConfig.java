package coffeeshout.minigame.infra.messaging.config;

import coffeeshout.cardgame.domain.event.dto.MiniGameStartedEvent;
import coffeeshout.minigame.application.MiniGamePersistenceService;
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
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MiniGameStreamEventConsumerConfig {

    private final Map<MiniGameType, MiniGameService> miniGameServiceMap;
    private final RoomQueryService roomQueryService;
    private final ApplicationEventPublisher eventPublisher;
    private final MiniGamePersistenceService miniGamePersistenceService;

    public MiniGameStreamEventConsumerConfig(
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

    @Bean
    public Consumer<StartMiniGameCommandEvent> startMiniGameCommandEventConsumer() {
        return event -> {
            log.info("[CONSUMER] 미니게임 시작 이벤트 수신: eventId={}, joinCode={}, hostName={}",
                    event.eventId(), event.joinCode(), event.hostName());

            final Room room = roomQueryService.getByJoinCode(new JoinCode(event.joinCode()));
            final Playable playable = room.startNextGame(event.hostName());
            eventPublisher.publishEvent(new MiniGameStartedEvent(event.joinCode(), playable.getMiniGameType().name()));
            miniGameServiceMap.get(playable.getMiniGameType()).start(event.joinCode(), event.hostName());
            miniGamePersistenceService.saveGameEntities(event, playable.getMiniGameType());
            log.info("[CONSUMER] JoinCode[{}] 미니게임 시작됨 - MiniGameType : {}", event.joinCode(), playable.getMiniGameType());
        };
    }
}
