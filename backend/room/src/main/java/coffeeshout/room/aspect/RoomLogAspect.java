package coffeeshout.room.aspect;

import coffeeshout.log.NotificationMarker;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Winner;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
@RequiredArgsConstructor
public class RoomLogAspect {

    private final Clock clock;

    @AfterReturning(
            value = "execution(* coffeeshout.room.application.service.RoomService.createRoom(..))",
            returning = "room"
    )
    public void logRoomCreation(Room room) {
        log.info(NotificationMarker.INSTANCE, "JoinCode[{}] 방 생성 완료 - host: {}, createdAt: {}",
                room.getJoinCode().getValue(),
                room.getHost().getName().value(),
                LocalDateTime.now(clock));
    }

    @AfterReturning(
            value = "execution(* coffeeshout.room.application.service.RoomService.spinRoulette(..)) && args(joinCode, hostName)",
            returning = "winner",
            argNames = "joinCode,hostName,winner"
    )
    public void logSpinRoulette(String joinCode, String hostName, Winner winner) {
        log.info(NotificationMarker.INSTANCE, "JoinCode[{}] 룰렛 추첨 완료 - 당첨자: {}, 호스트 : {}",
                joinCode,
                winner.name().value(),
                hostName);
    }

    @After(
            value = "execution(* coffeeshout.room.domain.repository.RoomRepository.deleteByJoinCode(..)) && args(joinCode)"
    )
    public void logDelayCleanUp(JoinCode joinCode) {
        log.info("JoinCode[{}] 방 삭제 완료", joinCode.getValue());
    }
}
