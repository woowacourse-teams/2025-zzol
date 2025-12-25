package coffeeshout.room.application.service;

import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import coffeeshout.room.domain.event.PlayerKickEvent;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.event.RouletteShowEvent;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.event.broadcast.MiniGameListChangedBroadcast;
import coffeeshout.room.domain.event.broadcast.PlayerListChangedBroadcast;
import coffeeshout.room.domain.event.broadcast.QrCodeStatusChangedBroadcast;
import coffeeshout.room.domain.event.broadcast.RouletteShownBroadcast;
import coffeeshout.room.domain.event.broadcast.RouletteWinnerSelectedBroadcast;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.service.MenuCommandService;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.infra.messaging.RoomEventWaitManager;
import coffeeshout.room.infra.persistence.RoulettePersistenceService;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomEventService {

    private final RoomCommandService roomCommandService;
    private final RoomQueryService roomQueryService;
    private final MenuCommandService menuCommandService;
    private final DelayedRoomRemovalService delayedRoomRemovalService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomEventWaitManager roomEventWaitManager;
    private final RoulettePersistenceService roulettePersistenceService;
    private final RouletteService rouletteService;

    public void updateMiniGames(MiniGameSelectEvent event) {
        log.info("JoinCode[{}] 미니게임 목록 업데이트 이벤트 처리 - 호스트: {}, 미니게임 종류: {}",
                event.joinCode(),
                event.hostName(),
                event.miniGameTypes()
        );

        List<MiniGameType> miniGameTypes = roomCommandService.updateMiniGames(
                new JoinCode(event.joinCode()),
                new PlayerName(event.hostName()),
                event.miniGameTypes()
        );

        eventPublisher.publishEvent(new MiniGameListChangedBroadcast(event.joinCode(), miniGameTypes));
    }

    public void kickPlayer(PlayerKickEvent event) {
        log.info("JoinCode[{}] 플레이어 강퇴 이벤트 처리 - 플레이어: {}", event.joinCode(), event.playerName());
        JoinCode joinCode = new JoinCode(event.joinCode());

        roomCommandService.removePlayer(joinCode, new PlayerName(event.playerName()));

        List<Player> players = roomQueryService.getPlayers(joinCode);

        eventPublisher.publishEvent(new PlayerListChangedBroadcast(event.joinCode(), players));
    }

    public void updatePlayers(PlayerListUpdateEvent event) {
        log.info("JoinCode[{}] 플레이어 목록 업데이트 이벤트 처리", event.joinCode());

        JoinCode joinCode = new JoinCode(event.joinCode());

        final List<Player> players = roomQueryService.getPlayers(joinCode);

        eventPublisher.publishEvent(new PlayerListChangedBroadcast(event.joinCode(), players));
    }

    public void readyPlayer(PlayerReadyEvent event) {
        log.info("JoinCode[{}] 플레이어 준비 상태 변경 이벤트 처리 - 플레이어: {}, 준비 상태: {}",
                event.joinCode(),
                event.playerName(),
                event.isReady()
        );

        Room room = roomCommandService.readyPlayer(new JoinCode(event.joinCode()),
                new PlayerName(event.playerName()),
                event.isReady()
        );

        eventPublisher.publishEvent(new PlayerListChangedBroadcast(event.joinCode(), room.getPlayers()));
    }

    public void joinRoom(RoomJoinEvent event) {
        log.info(" JoinCode[{}] 게스트 방 입장 이벤트 처리 - 게스트 이름: {}",
                event.joinCode(),
                event.guestName()
        );

        try {
            final SelectedMenuRequest selectedMenuRequest = event.selectedMenuRequest();

            final Menu menu = menuCommandService.convertMenu(
                    selectedMenuRequest.id(),
                    selectedMenuRequest.customName()
            );

            Room room = roomCommandService.joinGuest(
                    new JoinCode(event.joinCode()),
                    new PlayerName(event.guestName()),
                    menu, selectedMenuRequest.temperature()
            );

            roomEventWaitManager.notifySuccess(event.eventId(), room);
        } catch (Exception e) {
            roomEventWaitManager.notifyFailure(event.eventId(), e);
            throw e;
        }
    }

    public void createRoom(RoomCreateEvent event) {
        log.info(" JoinCode[{}] 방 생성 이벤트 처리 - 호스트 이름: {}",
                event.joinCode(),
                event.hostName()
        );

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
    }

    public void showRoulette(RouletteShowEvent event) {
        log.info("JoinCode[{}] 룰렛 화면 표시 이벤트 처리", event.joinCode());

        final RoomState roomState = rouletteService.showRoulette(event.joinCode());

        roulettePersistenceService.saveRoomStatus(event);

        eventPublisher.publishEvent(new RouletteShownBroadcast(event.joinCode(), roomState));
    }

    public void handleQrCodeStatus(QrCodeStatusEvent event) {
        log.info(
                "QR 코드 완료 이벤트 수신: eventId={}, joinCode={}, status={}",
                event.eventId(), event.joinCode(), event.status()
        );

        final QrCodeStatusChangedBroadcast broadCast = new QrCodeStatusChangedBroadcast(
                event.joinCode(),
                event.status(),
                event.qrCodeUrl()
        );

        switch (event.status()) {
            case SUCCESS -> {
                log.info(
                        "QR 코드 완료 이벤트 처리 완료 (SUCCESS): eventId={}, joinCode={}, url={}",
                        event.eventId(), event.joinCode(), event.qrCodeUrl()
                );
                roomCommandService.assignQrCode(new JoinCode(event.joinCode()), event.qrCodeUrl());
                eventPublisher.publishEvent(broadCast);
            }
            case ERROR -> {
                log.info(
                        "QR 코드 완료 이벤트 처리 완료 (ERROR): eventId={}, joinCode={}",
                        event.eventId(), event.joinCode()
                );
                roomCommandService.assignQrCodeError(new JoinCode(event.joinCode()));
                eventPublisher.publishEvent(broadCast);
            }
            default -> log.warn(
                    "처리할 수 없는 QR 코드 상태: eventId={}, joinCode={}, status={}",
                    event.eventId(), event.joinCode(), event.status()
            );
        }
    }

    public void spinRoulette(RouletteSpinEvent event) {
        final Winner winner = event.winner();
        roulettePersistenceService.saveRouletteResult(event);

        eventPublisher.publishEvent(new RouletteWinnerSelectedBroadcast(event.joinCode(), winner));
    }
}
