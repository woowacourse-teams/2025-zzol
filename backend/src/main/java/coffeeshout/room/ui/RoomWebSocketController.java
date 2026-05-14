package coffeeshout.room.ui;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.global.websocket.docs.WsTopic;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import coffeeshout.room.domain.event.RouletteShowEvent;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.ui.request.MiniGameSelectMessage;
import coffeeshout.room.ui.request.ReadyChangeMessage;
import coffeeshout.room.ui.request.RouletteSpinMessage;
import coffeeshout.room.ui.response.PlayerResponse;
import coffeeshout.room.ui.response.RoomStatusResponse;
import coffeeshout.room.ui.response.WinnerResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {

    private final StreamPublisher streamPublisher;
    private final RoomService roomService;

    @MessageMapping("/room/{joinCode}/update-players")
    @WsTopic(
            path = "/room/{joinCode}",
            payload = List.class,
            generic = PlayerResponse.class,
            description = "플레이어 목록 업데이트 및 브로드캐스트"
    )
    public void broadcastPlayers(@DestinationVariable String joinCode) {
        final BaseEvent event = new PlayerListUpdateEvent(joinCode);
        streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
    }

    @MessageMapping("/room/{joinCode}/update-ready")
    @WsTopic(
            path = "/room/{joinCode}",
            payload = List.class,
            generic = PlayerResponse.class,
            description = "플레이어 준비 상태 변경 및 브로드캐스트"
    )
    public void broadcastReady(@DestinationVariable String joinCode, ReadyChangeMessage message) {
        final BaseEvent event = new PlayerReadyEvent(joinCode, message.playerName(), message.isReady());
        streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
    }

    @MessageMapping("/room/{joinCode}/update-minigames")
    @WsTopic(
            path = "/room/{joinCode}/minigame",
            payload = List.class,
            generic = MiniGameType.class,
            description = "미니게임 목록 업데이트 및 브로드캐스트"
    )
    public void broadcastMiniGames(@DestinationVariable String joinCode, MiniGameSelectMessage message) {
        final BaseEvent event = new MiniGameSelectEvent(joinCode, message.hostName(),
                message.miniGameTypes());
        streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
    }

    @MessageMapping("/room/{joinCode}/show-roulette")
    @WsTopic(
            path = "/room/{joinCode}/roulette",
            payload = RoomStatusResponse.class,
            description = "룰렛 페이지로 이동"
    )
    public void broadcastShowRoulette(@DestinationVariable String joinCode) {
        final BaseEvent event = new RouletteShowEvent(joinCode);
        streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
    }

    @MessageMapping("/room/{joinCode}/spin-roulette")
    @WsTopic(
            path = "/room/{joinCode}/winner",
            payload = WinnerResponse.class,
            description = "룰렛 게임 실행 및 당첨자 발표"
    )
    public void broadcastRouletteSpin(@DestinationVariable String joinCode, RouletteSpinMessage message) {
        final Winner winner = roomService.spinRoulette(joinCode, message.hostName());
        final BaseEvent event = new RouletteSpinEvent(joinCode, message.hostName(), winner);
        streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);
    }
}
