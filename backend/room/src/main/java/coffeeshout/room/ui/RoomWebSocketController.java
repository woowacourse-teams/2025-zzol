package coffeeshout.room.ui;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.application.service.RoomService;
import coffeeshout.minigame.event.dto.MiniGameSelectEvent;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import coffeeshout.room.domain.event.RouletteShowEvent;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.room.ui.request.MiniGameSelectMessage;
import coffeeshout.room.ui.request.ReadyChangeMessage;
import coffeeshout.room.ui.request.RouletteSpinMessage;
import coffeeshout.websocket.docs.WsReceive;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {

    private final StreamPublisher streamPublisher;
    private final RoomService roomService;

    @MessageMapping("/room/{joinCode}/update-players")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}"},
            description = "플레이어 목록 업데이트 및 브로드캐스트"
    )
    public void broadcastPlayers(@DestinationVariable String joinCode) {
        final BaseEvent event = new PlayerListUpdateEvent(joinCode);
        streamPublisher.publish(RoomStreamKey.BROADCAST, event);
    }

    @MessageMapping("/room/{joinCode}/update-ready")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}"},
            description = "플레이어 준비 상태 변경 및 브로드캐스트"
    )
    public void broadcastReady(@DestinationVariable String joinCode, @Payload ReadyChangeMessage message) {
        final BaseEvent event = new PlayerReadyEvent(joinCode, message.playerName(), message.isReady());
        streamPublisher.publish(RoomStreamKey.BROADCAST, event);
    }

    @MessageMapping("/room/{joinCode}/update-minigames")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}/minigame"},
            description = "미니게임 목록 업데이트 및 브로드캐스트"
    )
    public void broadcastMiniGames(@DestinationVariable String joinCode, @Payload MiniGameSelectMessage message) {
        final BaseEvent event = new MiniGameSelectEvent(joinCode, message.hostName(),
                message.miniGameTypes());
        streamPublisher.publish(RoomStreamKey.BROADCAST, event);
    }

    @MessageMapping("/room/{joinCode}/show-roulette")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}/roulette"},
            description = "룰렛 페이지로 이동"
    )
    public void broadcastShowRoulette(@DestinationVariable String joinCode) {
        final BaseEvent event = new RouletteShowEvent(joinCode);
        streamPublisher.publish(RoomStreamKey.BROADCAST, event);
    }

    @MessageMapping("/room/{joinCode}/spin-roulette")
    @WsReceive(
            respondsOnTopics = {"/room/{joinCode}/winner"},
            description = "룰렛 게임 실행 및 당첨자 발표"
    )
    public void broadcastRouletteSpin(@DestinationVariable String joinCode, @Payload RouletteSpinMessage message) {
        final Winner winner = roomService.spinRoulette(joinCode, message.hostName());
        final BaseEvent event = new RouletteSpinEvent(joinCode, message.hostName(), winner);
        streamPublisher.publish(RoomStreamKey.BROADCAST, event);
    }
}
