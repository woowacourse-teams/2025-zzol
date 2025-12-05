package coffeeshout.room.infra.messaging;

import coffeeshout.global.exception.custom.InvalidArgumentException;
import coffeeshout.global.exception.custom.InvalidStateException;
import coffeeshout.global.infra.messaging.StreamEventHandler;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.RoomCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomJoinStreamHandler implements StreamEventHandler<RoomJoinEvent> {

    private final RoomCommandService roomCommandService;
    private final RoomEventWaitManager roomEventWaitManager;

    @Override
    public void handle(RoomJoinEvent event) {
        log.info("방 입장 이벤트 처리 시작: eventId={}, joinCode={}, guestName={}",
                event.eventId(), event.joinCode(), event.guestName());

        try {
            final Room room = roomCommandService.joinGuest(
                    new JoinCode(event.joinCode()),
                    new PlayerName(event.guestName())
            );

            log.info("방 입장 성공: joinCode={}, guestName={}, 현재 인원={}, eventId={}",
                    event.joinCode(), event.guestName(), room.getPlayers().size(), event.eventId());

            roomEventWaitManager.notifySuccess(event.eventId(), room);

        } catch (InvalidArgumentException | InvalidStateException e) {
            log.warn("방 입장 처리 중 비즈니스 오류: joinCode={}, guestName={}, eventId={}, error={}",
                    event.joinCode(), event.guestName(), event.eventId(), e.getMessage());
            roomEventWaitManager.notifyFailure(event.eventId(), e);
            throw e;

        } catch (Exception e) {
            log.error("방 입장 처리 중 시스템 오류: joinCode={}, guestName={}, eventId={}",
                    event.joinCode(), event.guestName(), event.eventId(), e);
            roomEventWaitManager.notifyFailure(event.eventId(), e);
            throw e;
        }
    }
}
