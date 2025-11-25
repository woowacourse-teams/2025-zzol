package coffeeshout.global.log;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import java.time.LocalDateTime;
import java.util.List;
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
            value = "execution(* coffeeshout.room.application.RoomService.createRoom(..))",
            returning = "room"
    )
    public void logRoomCreation(Room room) {
        log.info(NOTIFICATION_MARKER, "JoinCode[{}] 방 생성 완료 - host: {}, createdAt: {}",
                room.getJoinCode().getValue(),
                room.getHost().getName().value(),
                LocalDateTime.now());
    }

    @After(
            value = "execution(* coffeeshout.room.application.RoomService.changePlayerReadyState(..)) && args(joinCode, playerName, isReady)",
            argNames = "joinCode,playerName,isReady"
    )
    public void logPlayerReadyState(String joinCode, String playerName, Boolean isReady) {
        log.info("JoinCode[{}] 플레이어 Ready 상태 변경 - 플레이어: {}, 상태: {}", joinCode, playerName, isReady);
    }

    @AfterReturning(
            value = "execution(* coffeeshout.room.application.RoomService.spinRoulette(..)) && args(joinCode, hostName)",
            returning = "winner",
            argNames = "joinCode,hostName,winner"
    )
    public void logSpinRoulette(String joinCode, String hostName, Winner winner) {
        log.info(NOTIFICATION_MARKER, "JoinCode[{}] 룰렛 추첨 완료 - 당첨자: {}",
                joinCode,
                winner.name().value());
    }

    @After(
            value = "execution(* coffeeshout.room.domain.repository.RoomRepository.deleteByJoinCode(..)) && args(joinCode)"
    )
    public void logDelayCleanUp(JoinCode joinCode) {
        log.info("JoinCode[{}] 방 삭제 완료", joinCode.getValue());
    }

    @AfterReturning(
            value = "execution(* coffeeshout.room.application.RoomService.enterRoom(..)) && args(joinCode, guestName, selectedMenuRequest)",
            returning = "room",
            argNames = "joinCode,guestName,selectedMenuRequest,room"
    )
    public void logEnterRoom(String joinCode, String guestName, SelectedMenuRequest selectedMenuRequest, Room room) {
        final List<String> playerNames = room.getPlayers().stream()
                .map(player -> player.getName().value())
                .toList();
        log.info("JoinCode[{}] 게스트 입장 - 게스트 이름: {}, 메뉴 ID: {}, 현재 참여자 목록: {}", joinCode, guestName,
                selectedMenuRequest.id(),
                playerNames);
    }

    @After(
            value = "execution(* coffeeshout.room.application.RoomService.selectMenu(..)) && args(joinCode, guestName, menuId)",
            argNames = "joinCode,guestName,menuId"
    )
    public void logSelectMenu(String joinCode, String guestName, Long menuId) {
        log.info("JoinCode[{}] 메뉴 변경 - 게스트 이름: {}, 메뉴 ID: {}", joinCode, guestName, menuId);
    }
}
