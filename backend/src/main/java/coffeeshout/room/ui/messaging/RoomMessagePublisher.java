package coffeeshout.room.ui.messaging;

import coffeeshout.global.websocket.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import coffeeshout.room.domain.event.RouletteShownEvent;
import coffeeshout.room.domain.event.RouletteWinnerEvent;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.ui.response.PlayerResponse;
import coffeeshout.room.ui.response.QrCodeStatusResponse;
import coffeeshout.room.ui.response.RoomStatusResponse;
import coffeeshout.room.ui.response.WinnerResponse;
import generator.annotaions.MessageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomMessagePublisher {

    private static final String PLAYER_LIST_TOPIC_FORMAT = "/topic/room/%s";
    private static final String MINI_GAME_TOPIC_FORMAT = "/topic/room/%s/minigame";
    private static final String ROULETTE_TOPIC_FORMAT = "/topic/room/%s/roulette";
    private static final String WINNER_TOPIC_FORMAT = "/topic/room/%s/winner";
    private static final String QR_CODE_TOPIC_FORMAT = "/topic/room/%s/qr-code";

    private final LoggingSimpMessagingTemplate messagingTemplate;
    private final RoomQueryService roomQueryService;

    @EventListener
    @MessageResponse(
            path = "/room/{joinCode}",
            returnType = List.class,
            genericType = PlayerResponse.class
    )
    public void onPlayerListChanged(PlayerListUpdateEvent event) {
        log.debug("플레이어 목록 변경 이벤트 수신: joinCode={}",
                event.joinCode());

        final List<PlayerResponse> responses = roomQueryService.getPlayers(new JoinCode(event.joinCode()))
                .stream()
                .map(PlayerResponse::from)
                .toList();

        final String destination = String.format(PLAYER_LIST_TOPIC_FORMAT, event.joinCode());
        messagingTemplate.convertAndSend(destination, WebSocketResponse.success(responses));

        log.debug("플레이어 목록 브로드캐스트 완료: joinCode={}", event.joinCode());
    }

    @EventListener
    @MessageResponse(
            path = "/room/{joinCode}/minigame",
            returnType = List.class,
            genericType = String.class
    )
    public void onMiniGameListChanged(MiniGameSelectEvent event) {
        log.debug("미니게임 목록 변경 이벤트 수신: joinCode={}, gameCount={}",
                event.joinCode(), event.miniGameTypes().size());

        final String destination = String.format(MINI_GAME_TOPIC_FORMAT, event.joinCode());
        messagingTemplate.convertAndSend(destination, WebSocketResponse.success(event.miniGameTypes()));

        log.debug("미니게임 목록 브로드캐스트 완료: joinCode={}", event.joinCode());
    }

    @EventListener
    @MessageResponse(
            path = "/room/{joinCode}/roulette",
            returnType = RoomStatusResponse.class
    )
    public void onRouletteShown(RouletteShownEvent event) {
        log.debug("룰렛 화면 표시 이벤트 수신: joinCode={}, roomState={}",
                event.joinCode(), event.roomState());

        final RoomStatusResponse response = RoomStatusResponse.of(new JoinCode(event.joinCode()), event.roomState());
        final String destination = String.format(ROULETTE_TOPIC_FORMAT, event.joinCode());
        messagingTemplate.convertAndSend(destination, WebSocketResponse.success(response));

        log.debug("룰렛 화면 전환 브로드캐스트 완료: joinCode={}", event.joinCode());
    }

    @EventListener
    @MessageResponse(
            path = "/room/{joinCode}/winner",
            returnType = WinnerResponse.class
    )
    public void onRouletteWinnerSelected(RouletteWinnerEvent event) {
        log.debug("룰렛 당첨자 선택 이벤트 수신: joinCode={}, winner={}",
                event.joinCode(), event.winner().name().value());

        final WinnerResponse response = WinnerResponse.from(event.winner());
        final String destination = String.format(WINNER_TOPIC_FORMAT, event.joinCode());
        messagingTemplate.convertAndSend(destination, WebSocketResponse.success(response));

        log.debug("룰렛 당첨자 브로드캐스트 완료: joinCode={}, winner={}",
                event.joinCode(), event.winner().name().value());
    }

    @EventListener
    @MessageResponse(
            path = "/room/{joinCode}/qr-code",
            returnType = QrCodeStatusResponse.class
    )
    public void onQrCodeStatusChanged(QrCodeStatusEvent event) {
        log.debug("QR 코드 상태 변경 이벤트 수신: joinCode={}, status={}",
                event.joinCode(), event.status());

        final QrCodeStatusResponse response = new QrCodeStatusResponse(event.status(), event.qrCodeUrl());
        final String destination = String.format(QR_CODE_TOPIC_FORMAT, event.joinCode());
        messagingTemplate.convertAndSend(destination, WebSocketResponse.success(response));

        log.debug("QR 코드 상태 브로드캐스트 완료: joinCode={}, status={}", event.joinCode(), event.status());
    }
}

