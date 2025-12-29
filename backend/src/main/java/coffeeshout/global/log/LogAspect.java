package coffeeshout.global.log;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Winner;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class LogAspect {

    public static final Marker NOTIFICATION_MARKER = MarkerFactory.getMarker("[NOTIFICATION]");

    @AfterReturning(
            value = "execution(* coffeeshout.room.application.service.RoomService.createRoom(..))",
            returning = "room"
    )
    public void logRoomCreation(Room room) {
        log.info(NOTIFICATION_MARKER, "JoinCode[{}] 방 생성 완료 - host: {}, createdAt: {}",
                room.getJoinCode().getValue(),
                room.getHost().getName().value(),
                LocalDateTime.now());
    }

    @AfterReturning(
            value = "execution(* coffeeshout.room.application.service.RoomService.spinRoulette(..)) && args(joinCode, hostName)",
            returning = "winner",
            argNames = "joinCode,hostName,winner"
    )
    public void logSpinRoulette(String joinCode, String hostName, Winner winner) {
        log.info(NOTIFICATION_MARKER, "JoinCode[{}] 룰렛 추첨 완료 - 당첨자: {}, 호스트 : {}",
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


    @After(
            value = "execution(* coffeeshout.room.application.service.RoomService.selectMenu(..)) && args(joinCode, guestName, menuId)",
            argNames = "joinCode,guestName,menuId"
    )
    public void logSelectMenu(String joinCode, String guestName, Long menuId) {
        log.info("JoinCode[{}] 메뉴 변경 - 게스트 이름: {}, 메뉴 ID: {}", joinCode, guestName, menuId);
    }
}
