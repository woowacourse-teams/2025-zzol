package coffeeshout.room.application;

import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.QrCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.PlayerKickEvent;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.menu.MenuTemperature;
import coffeeshout.room.domain.menu.SelectedMenu;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.roulette.Roulette;
import coffeeshout.room.domain.roulette.RoulettePicker;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.room.domain.service.MenuCommandService;
import coffeeshout.room.domain.service.MenuQueryService;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.infra.messaging.RoomEnterStreamProducer;
import coffeeshout.room.infra.messaging.RoomEventPublisher;
import coffeeshout.room.infra.messaging.RoomEventWaitManager;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import coffeeshout.room.ui.request.SelectedMenuRequest;
import coffeeshout.room.ui.response.ProbabilityResponse;
import coffeeshout.room.ui.response.QrCodeStatusResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomService {

    private final RoomQueryService roomQueryService;
    private final RoomCommandService roomCommandService;
    private final MenuQueryService menuQueryService;
    private final QrCodeService qrCodeService;
    private final JoinCodeGenerator joinCodeGenerator;
    private final RoomEventPublisher roomEventPublisher;
    private final RoomEventWaitManager roomEventWaitManager;
    private final MenuCommandService menuCommandService;
    private final RoomEnterStreamProducer roomEnterStreamProducer;
    private final RoomJpaRepository roomJpaRepository;

    @Value("${room.event.timeout:PT5S}")
    private Duration eventTimeout;

    @Transactional
    public Room createRoom(String hostName, SelectedMenuRequest selectedMenuRequest) {
        final JoinCode joinCode = joinCodeGenerator.generate();

        // 방 생성 (QR 코드는 PENDING 상태로 시작)
        final Menu menu = menuCommandService.convertMenu(selectedMenuRequest.id(), selectedMenuRequest.customName());
        final Room room = roomCommandService.saveIfAbsentRoom(joinCode, new PlayerName(hostName),
                menu, selectedMenuRequest.temperature());

        // 방 생성 후 이벤트 전달
        final RoomCreateEvent event = new RoomCreateEvent(
                hostName,
                selectedMenuRequest,
                joinCode.getValue()
        );

        roomEventPublisher.publishEvent(event);

        // QR 코드 비동기 생성 시작
        qrCodeService.generateQrCodeAsync(joinCode.getValue());

        saveRoomEntity(joinCode.getValue());

        log.info("방 생성 이벤트 처리 완료 (DB 저장): eventId={}, joinCode={}",
                event.eventId(), event.joinCode());

        // 해당 방 정보 수신
        return room;
    }

    // === 비동기 메서드들 (REST Controller용) ===

    public CompletableFuture<Room> enterRoomAsync(
            String joinCode,
            String guestName,
            SelectedMenuRequest selectedMenuRequest
    ) {
        final RoomJoinEvent event = new RoomJoinEvent(joinCode, guestName, selectedMenuRequest);

        return processEventAsync(
                event.eventId(),
                () -> roomEnterStreamProducer.broadcastEnterRoom(event),
                "방 참가",
                String.format("joinCode=%s, guestName=%s", joinCode, guestName),
                room -> String.format("joinCode=%s, guestName=%s", joinCode, guestName)
        );
    }

    private <T> CompletableFuture<T> processEventAsync(
            String eventId,
            Runnable eventPublisher,
            String operationName,
            String logParams,
            Function<T, String> successLogParams
    ) {
        final CompletableFuture<T> future = roomEventWaitManager.registerWait(eventId);

        try {
            eventPublisher.run();
        } catch (Exception e) {
            log.error("{} 이벤트 발행 실패: eventId={}, {}", operationName, eventId, logParams, e);
            future.completeExceptionally(e);
            return future;
        }

        return future.orTimeout(eventTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("{} 비동기 처리 실패: eventId={}, {}",
                                operationName, eventId, logParams, throwable);
                        return;
                    }
                    log.info("{} 비동기 처리 완료: {}, eventId={}",
                            operationName, successLogParams.apply(result), eventId);
                });
    }

    public List<Player> changePlayerReadyState(String joinCode, String playerName, Boolean isReady) {
        return changePlayerReadyStateInternal(joinCode, playerName, isReady);
    }

    public Winner spinRoulette(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        Player host = room.findPlayer(new PlayerName(hostName));

        return room.spinRoulette(host, new Roulette(new RoulettePicker()));
    }

    public Room getRoomByJoinCode(String joinCode) {
        return roomQueryService.getByJoinCode(new JoinCode(joinCode));
    }

    public List<Player> getPlayersInternal(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.getPlayers();
    }

    public void saveRoomEntity(String joinCodeValue) {
        final RoomEntity roomEntity = new RoomEntity(joinCodeValue);
        roomJpaRepository.save(roomEntity);
    }

    public Room enterRoom(String joinCode, String guestName, SelectedMenuRequest selectedMenuRequest) {
        Menu menu = menuCommandService.convertMenu(selectedMenuRequest.id(), selectedMenuRequest.customName());

        return roomCommandService.joinGuest(
                new JoinCode(joinCode),
                new PlayerName(guestName),
                menu,
                selectedMenuRequest.temperature()
        );
    }

    public List<Player> changePlayerReadyStateInternal(String joinCode, String playerName, Boolean isReady) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final Player player = room.findPlayer(new PlayerName(playerName));

        if (player.getPlayerType() == PlayerType.HOST) {
            return room.getPlayers();
        }

        player.updateReadyState(isReady);
        roomCommandService.save(room);
        return room.getPlayers();
    }

    public List<MiniGameType> updateMiniGamesInternal(String joinCode, String hostName,
                                                      List<MiniGameType> miniGameTypes) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        room.clearMiniGames();

        miniGameTypes.forEach(miniGameType -> {
            final Playable miniGame = miniGameType.createMiniGame(joinCode);
            room.addMiniGame(new PlayerName(hostName), miniGame);
        });

        roomCommandService.save(room);

        return room.getAllMiniGame().stream()
                .map(Playable::getMiniGameType)
                .toList();
    }

    // === 나머지 기존 메서드들 (변경 없음) ===

    public List<Player> getAllPlayers(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));

        return room.getPlayers();
    }

    public List<Player> selectMenu(String joinCode, String playerName, Long menuId) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final Menu menu = menuQueryService.getById(menuId);

        final Player player = room.findPlayer(new PlayerName(playerName));
        player.selectMenu(new SelectedMenu(menu, MenuTemperature.ICE));

        return room.getPlayers();
    }

    public List<MiniGameType> updateMiniGames(String joinCode, String hostName, List<MiniGameType> miniGameTypes) {
        return updateMiniGamesInternal(joinCode, hostName, miniGameTypes);
    }

    public List<MiniGameType> getAllMiniGames() {
        return Arrays.stream(MiniGameType.values())
                .toList();
    }

    public boolean roomExists(String joinCode) {
        return roomQueryService.existsByJoinCode(new JoinCode(joinCode));
    }

    public boolean isGuestNameDuplicated(String joinCode, String guestName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));

        return room.hasDuplicatePlayerName(new PlayerName(guestName));
    }

    public List<ProbabilityResponse> getProbabilities(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.getPlayers().stream()
                .map(ProbabilityResponse::from)
                .toList();
    }

    public Map<Player, MiniGameScore> getMiniGameScores(String joinCode, MiniGameType miniGameType) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final Playable miniGame = room.findMiniGame(miniGameType);

        return miniGame.getScores();
    }

    public MiniGameResult getMiniGameRanks(String joinCode, MiniGameType miniGameType) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final Playable miniGame = room.findMiniGame(miniGameType);

        return miniGame.getResult();
    }

    public List<MiniGameType> getSelectedMiniGames(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.getSelectedMiniGameTypes();
    }

    public boolean removePlayer(String joinCode, String playerName) {
        final JoinCode code = new JoinCode(joinCode);
        final Room room = roomQueryService.getByJoinCode(code);

        boolean isRemoved = room.removePlayer(new PlayerName(playerName));
        if (room.isEmpty()) {
            roomCommandService.delete(code);
        }
        return isRemoved;
    }

    public boolean isReadyState(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.isReadyState();
    }

    public Room showRoulette(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        room.showRoulette();
        return room;
    }

    public QrCodeStatusResponse getQrCodeStatus(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final QrCode qrCode = room.getJoinCode().getQrCode();

        QrCodeStatusResponse response = new QrCodeStatusResponse(qrCode.getStatus(), qrCode.getUrl());

        log.debug("QR 코드 상태 반환: joinCode={}, status={}", joinCode, qrCode.getStatus());
        return response;
    }

    public boolean kickPlayer(String joinCode, String playerName) {
        final boolean exists = hasPlayer(joinCode, playerName);

        if (exists) {
            final PlayerKickEvent event = new PlayerKickEvent(joinCode, playerName);
            roomEventPublisher.publishEvent(event);
        }

        return exists;
    }

    private boolean hasPlayer(String joinCode, String playerName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.hasPlayer(new PlayerName(playerName));
    }

    public List<Playable> getRemainingMiniGames(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.getMiniGames().stream().toList();
    }
}
