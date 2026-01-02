package coffeeshout.room.application.service;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.event.RouletteShowEvent;
import coffeeshout.room.domain.event.RouletteShownEvent;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.event.RouletteWinnerEvent;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.infra.messaging.RoomEventWaitManager;
import coffeeshout.room.infra.persistence.RoulettePersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomEventService {

    private final RoomCommandService roomCommandService;
    private final DelayedRoomRemovalService delayedRoomRemovalService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomEventWaitManager roomEventWaitManager;
    private final RoulettePersistenceService roulettePersistenceService;
    private final RouletteService rouletteService;

    public void joinRoom(RoomJoinEvent event) {
        log.info("JoinCode[{}] 게스트 방 입장 이벤트 처리 - 게스트 이름: {}",
                event.joinCode(),
                event.guestName()
        );

        try {
            final Room room = roomCommandService.joinGuest(
                    new JoinCode(event.joinCode()),
                    new PlayerName(event.guestName())
            );

            roomEventWaitManager.notifySuccess(event.eventId(), room);
        } catch (Exception e) {
            roomEventWaitManager.notifyFailure(event.eventId(), e);
            throw e;
        }
    }

    public void createRoom(RoomCreateEvent event) {
        log.info("JoinCode[{}] 방 생성 이벤트 처리 - 호스트 이름: {}",
                event.joinCode(),
                event.hostName()
        );

        roomCommandService.saveIfAbsentRoom(
                new JoinCode(event.joinCode()),
                new PlayerName(event.hostName())
        );

        delayedRoomRemovalService.scheduleRemoveRoom(new JoinCode(event.joinCode()));
    }

    public void showRoulette(RouletteShowEvent event) {
        log.info("JoinCode[{}] 룰렛 화면 표시 이벤트 처리", event.joinCode());

        final RoomState roomState = rouletteService.showRoulette(event.joinCode());

        roulettePersistenceService.saveRoomStatus(event);

        eventPublisher.publishEvent(new RouletteShownEvent(event.joinCode(), roomState));
    }

    public void spinRoulette(RouletteSpinEvent event) {
        log.info("JoinCode[{}] 룰렛 스핀 이벤트 처리 - 당첨자: {}", event.joinCode(), event.winner().name().value());

        final Winner winner = event.winner();
        roulettePersistenceService.saveRouletteResult(event);

        eventPublisher.publishEvent(new RouletteWinnerEvent(event.joinCode(), winner));
    }
}
