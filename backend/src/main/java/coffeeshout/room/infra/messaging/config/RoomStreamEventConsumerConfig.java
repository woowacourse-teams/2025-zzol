package coffeeshout.room.infra.messaging.config;

import coffeeshout.global.ui.WebSocketResponse;
import coffeeshout.global.websocket.LoggingSimpMessagingTemplate;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.DelayedRoomRemovalService;
import coffeeshout.room.application.RoomService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import coffeeshout.room.domain.event.PlayerKickEvent;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.event.RouletteShowEvent;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.service.MenuCommandService;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.infra.messaging.RoomEventWaitManager;
import coffeeshout.room.infra.messaging.handler.RoulettePersistenceService;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import coffeeshout.room.ui.response.PlayerResponse;
import coffeeshout.room.ui.response.QrCodeStatusResponse;
import coffeeshout.room.ui.response.RoomStatusResponse;
import coffeeshout.room.ui.response.WinnerResponse;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RoomStreamEventConsumerConfig {

    private final RoomCommandService roomCommandService;
    private final MenuCommandService menuCommandService;
    private final RoomEventWaitManager roomEventWaitManager;
    private final DelayedRoomRemovalService delayedRoomRemovalService;

    @Bean
    public Consumer<RoomJoinEvent> roomJoinEventConsumer() {
        return event -> {
            try {
                final SelectedMenuRequest selectedMenuRequest = event.selectedMenuRequest();

                final Menu menu = menuCommandService.convertMenu(
                        selectedMenuRequest.id(),
                        selectedMenuRequest.customName()
                );

                final Room room = roomCommandService.joinGuest(
                        new JoinCode(event.joinCode()),
                        new PlayerName(event.guestName()),
                        menu, selectedMenuRequest.temperature()
                );
                roomEventWaitManager.notifySuccess(event.eventId(), room);
            } catch (Exception e) {
                roomEventWaitManager.notifyFailure(event.eventId(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<RoomCreateEvent> roomCreateEventConsumer() {
        return event -> {
            final SelectedMenuRequest selectedMenuRequest = event.selectedMenuRequest();
            final Menu menu = menuCommandService.convertMenu(
                    selectedMenuRequest.id(),
                    selectedMenuRequest.customName()
            );

            roomCommandService.saveIfAbsentRoom(
                    new JoinCode(event.joinCode()),
                    new PlayerName(event.hostName()),
                    menu,
                    selectedMenuRequest.temperature()
            );

            delayedRoomRemovalService.scheduleRemoveRoom(new JoinCode(event.joinCode()));
        };
    }

    @Bean
    public Consumer<MiniGameSelectEvent> miniGameSelectEventConsumer(
            RoomService roomService,
            LoggingSimpMessagingTemplate messagingTemplate
    ) {
        return event -> {
            final List<MiniGameType> selectedMiniGames =
                    roomService.updateMiniGamesInternal(
                            event.joinCode(),
                            event.hostName(),
                            event.miniGameTypes()
                    );

            messagingTemplate.convertAndSend("/topic/room/" + event.joinCode() + "/minigame",
                    WebSocketResponse.success(selectedMiniGames));
        };
    }

    @Bean
    public Consumer<PlayerKickEvent> playerKickEventConsumer(
            RoomService roomService,
            LoggingSimpMessagingTemplate messagingTemplate
    ) {
        return event -> {
            roomService.removePlayer(event.joinCode(), event.playerName());

            final List<Player> players =
                    roomService.getPlayersInternal(event.joinCode());
            final List<PlayerResponse> responses = players.stream()
                    .map(PlayerResponse::from)
                    .toList();

            messagingTemplate.convertAndSend(
                    "/topic/room/" + event.joinCode(),
                    WebSocketResponse.success(responses)
            );
        };
    }

    @Bean
    public Consumer<PlayerListUpdateEvent> playerListUpdateEventConsumer(
            RoomService roomService,
            LoggingSimpMessagingTemplate messagingTemplate
    ) {
        return event -> {
            final List<Player> players = roomService.getPlayersInternal(event.joinCode());
            final List<PlayerResponse> responses = players.stream()
                    .map(PlayerResponse::from)
                    .toList();

            messagingTemplate.convertAndSend("/topic/room/" + event.joinCode(),
                    WebSocketResponse.success(responses));
        };
    }

    @Bean
    public Consumer<PlayerReadyEvent> playerReadyEventConsumer(
            RoomService roomService,
            LoggingSimpMessagingTemplate messagingTemplate
    ) {
        return event -> {
            final List<Player> players = roomService.changePlayerReadyStateInternal(
                    event.joinCode(),
                    event.playerName(),
                    event.isReady()
            );
            final List<PlayerResponse> responses = players.stream()
                    .map(PlayerResponse::from)
                    .toList();

            messagingTemplate.convertAndSend("/topic/room/" + event.joinCode(),
                    WebSocketResponse.success(responses));
        };
    }

    @Bean
    public Consumer<QrCodeStatusEvent> qrCodeStatusEventConsumer(
            RoomCommandService roomCommandService,
            LoggingSimpMessagingTemplate messagingTemplate
    ) {
        return event -> {
            LoggerFactory.getLogger(getClass()).info(
                    "QR 코드 완료 이벤트 수신: eventId={}, joinCode={}, status={}",
                    event.eventId(), event.joinCode(), event.status()
            );

            switch (event.status()) {
                case SUCCESS -> {
                    LoggerFactory.getLogger(getClass()).info(
                            "QR 코드 완료 이벤트 처리 완료 (SUCCESS): eventId={}, joinCode={}, url={}",
                            event.eventId(), event.joinCode(), event.qrCodeUrl()
                    );
                    roomCommandService.assignQrCode(new JoinCode(event.joinCode()), event.qrCodeUrl());
                    sendQrCode(event, messagingTemplate);
                }
                case ERROR -> {
                    LoggerFactory.getLogger(getClass()).info(
                            "QR 코드 완료 이벤트 처리 완료 (ERROR): eventId={}, joinCode={}",
                            event.eventId(), event.joinCode()
                    );
                    roomCommandService.assignQrCodeError(new JoinCode(event.joinCode()));
                    sendQrCode(event, messagingTemplate);
                }
                default -> LoggerFactory.getLogger(getClass()).warn(
                        "처리할 수 없는 QR 코드 상태: eventId={}, joinCode={}, status={}",
                        event.eventId(), event.joinCode(), event.status()
                );
            }
        };
    }

    @Bean
    public Consumer<RouletteShowEvent> rouletteShowEventConsumer(
            RoomService roomService,
            RoulettePersistenceService roulettePersistenceService,
            LoggingSimpMessagingTemplate messagingTemplate
    ) {
        return event -> {
            final Room room = roomService.showRoulette(event.joinCode());
            final RoomStatusResponse response = RoomStatusResponse.of(room.getJoinCode(), room.getRoomState());

            messagingTemplate.convertAndSend("/topic/room/" + event.joinCode() + "/roulette",
                    WebSocketResponse.success(response));
            roulettePersistenceService.saveRoomStatus(event);
        };
    }

    @Bean
    public Consumer<RouletteSpinEvent> rouletteSpinEventConsumer(
            RoulettePersistenceService roulettePersistenceService,
            LoggingSimpMessagingTemplate messagingTemplate
    ) {
        return event -> {
            final Winner winner = event.winner();
            final WinnerResponse response = WinnerResponse.from(winner);

            messagingTemplate.convertAndSend("/topic/room/" + event.joinCode() + "/winner",
                    WebSocketResponse.success(response));
            roulettePersistenceService.saveRouletteResult(event);
        };
    }

    private void sendQrCode(
            QrCodeStatusEvent event,
            LoggingSimpMessagingTemplate messagingTemplate
    ) {
        final QrCodeStatusResponse response = new QrCodeStatusResponse(event.status(), event.qrCodeUrl());

        final String destination = String.format("/topic/room/%s/qr-code", event.joinCode());
        messagingTemplate.convertAndSend(destination, WebSocketResponse.success(response));

        LoggerFactory.getLogger(getClass()).debug(
                "QR 코드 이벤트 전송 완료: destination={}, status={}, url={}",
                destination, event.status(), event.qrCodeUrl()
        );
    }
}
