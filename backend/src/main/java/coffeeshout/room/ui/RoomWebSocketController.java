package coffeeshout.room.ui;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import coffeeshout.room.domain.event.RouletteShowEvent;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.roulette.Roulette;
import coffeeshout.room.domain.roulette.RoulettePicker;
import coffeeshout.room.infra.messaging.RoomEventPublisher;
import coffeeshout.room.ui.request.MiniGameSelectMessage;
import coffeeshout.room.ui.request.ReadyChangeMessage;
import coffeeshout.room.ui.request.RouletteSpinMessage;
import coffeeshout.room.ui.response.PlayerResponse;
import coffeeshout.room.ui.response.RoomStatusResponse;
import coffeeshout.room.ui.response.WinnerResponse;
import generator.annotaions.MessageResponse;
import generator.annotaions.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {

    private final RoomEventPublisher roomEventPublisher;
    private final RoomService roomService;

    @MessageMapping("/room/{joinCode}/update-players")
    @MessageResponse(
            path = "/room/{joinCode}",
            returnType = List.class,
            genericType = PlayerResponse.class
    )
    @Operation(
            summary = "플레이어 목록 업데이트 및 브로드캐스트",
            description = """
                    방의 플레이어 목록을 업데이트하고 모든 참가자에게 브로드캐스트합니다.
                    방에 참가한 모든 플레이어들의 정보를 조회하여 실시간으로 공유합니다.
                    """
    )
    public void broadcastPlayers(@DestinationVariable String joinCode) {
        final PlayerListUpdateEvent event = new PlayerListUpdateEvent(joinCode);
        roomEventPublisher.publishEvent(event);
    }

    @MessageMapping("/room/{joinCode}/update-ready")
    @MessageResponse(
            path = "/room/{joinCode}",
            returnType = List.class,
            genericType = PlayerResponse.class
    )
    @Operation(
            summary = "플레이어 준비 상태 변경 및 브로드캐스트",
            description = """
                    플레이어의 준비 상태를 변경하고 업데이트된 플레이어 목록을 브로드캐스트합니다.
                    플레이어가 게임 준비 완료 또는 준비 취소를 할 때 해당 상태를 저장하고
                    모든 참가자에게 변경된 준비 상태를 실시간으로 전달합니다.
                    """
    )
    public void broadcastReady(@DestinationVariable String joinCode, ReadyChangeMessage message) {
        final PlayerReadyEvent event = new PlayerReadyEvent(joinCode, message.playerName(), message.isReady());
        roomEventPublisher.publishEvent(event);
    }

    @MessageMapping("/room/{joinCode}/update-minigames")
    @MessageResponse(
            path = "/room/{joinCode}/minigame",
            returnType = List.class,
            genericType = MiniGameType.class
    )
    @Operation(
            summary = "미니게임 목록 업데이트 및 브로드캐스트",
            description = """
                    호스트가 선택한 미니게임 목록을 업데이트하고 모든 참가자에게 브로드캐스트합니다.
                    방장이 게임에서 사용할 미니게임들을 선택하면 해당 정보를 저장하고
                    미니게임 채널을 통해 선택된 게임 타입들을 실시간으로 전달합니다.
                    """
    )
    public void broadcastMiniGames(@DestinationVariable String joinCode, MiniGameSelectMessage message) {
        final MiniGameSelectEvent event = new MiniGameSelectEvent(joinCode, message.hostName(),
                message.miniGameTypes());
        roomEventPublisher.publishEvent(event);
    }

    @MessageMapping("/room/{joinCode}/show-roulette")
    @MessageResponse(
            path = "/room/{joinCode}/roulette",
            returnType = RoomStatusResponse.class
    )
    @Operation(
            summary = "룰렛 페이지로 이동",
            description = """
                    호스트가 룰렛 페이지로 플레이어 전부를 이동시킵니다.
                    """
    )
    public void broadcastShowRoulette(@DestinationVariable String joinCode) {
        final RouletteShowEvent event = new RouletteShowEvent(joinCode);
        roomEventPublisher.publishEvent(event);
    }

    @MessageMapping("/room/{joinCode}/spin-roulette")
    @MessageResponse(
            path = "/room/{joinCode}/winner",
            returnType = WinnerResponse.class
    )
    @Operation(
            summary = "룰렛 게임 실행 및 당첨자 발표",
            description = """
                    호스트가 룰렛을 돌려서 당첨자를 결정하고 결과를 모든 참가자에게 브로드캐스트합니다.
                    """
    )
    public void broadcastRouletteSpin(@DestinationVariable String joinCode, RouletteSpinMessage message) {
        final Room room = roomService.getRoomByJoinCode(joinCode);
        final Winner winner = room.spinRoulette(room.getHost(), new Roulette(new RoulettePicker()));
        final RouletteSpinEvent event = new RouletteSpinEvent(joinCode, message.hostName(), winner);
        roomEventPublisher.publishEvent(event);
    }
}
