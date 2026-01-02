package coffeeshout.room.application.service;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.QrCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.MiniGameSelectEvent;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.roulette.Roulette;
import coffeeshout.room.domain.roulette.RoulettePicker;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.infra.messaging.RoomEventWaitManager;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomQueryService roomQueryService;
    private final RoomCommandService roomCommandService;
    private final QrCodeService qrCodeService;
    private final JoinCodeGenerator joinCodeGenerator;
    private final RoomEventWaitManager roomEventWaitManager;
    private final RoomJpaRepository roomJpaRepository;
    private final StreamPublisher streamPublisher;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${room.event.timeout:PT5S}")
    private Duration eventTimeout;

    @Transactional
    public Room createRoom(String hostName) {
        final JoinCode joinCode = joinCodeGenerator.generate();

        // 방 생성 (QR 코드는 PENDING 상태로 시작)
        final Room room = roomCommandService.saveIfAbsentRoom(joinCode, new PlayerName(hostName));

        // 방 생성 후 이벤트 전달
        final BaseEvent event = new RoomCreateEvent(hostName, joinCode.getValue());

        streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);

        // QR 코드 비동기 생성 시작
        qrCodeService.generateQrCodeAsync(joinCode.getValue());

        saveRoomEntity(joinCode.getValue());

        log.info("방 생성 이벤트 처리 완료 (DB 저장): eventId={}, joinCode={}",
                event.eventId(), joinCode.getValue());

        // 해당 방 정보 수신
        return room;
    }

    public CompletableFuture<Room> enterRoomAsync(String joinCode, String guestName) {
        final RoomJoinEvent event = new RoomJoinEvent(joinCode, guestName);

        return processEventAsync(
                event.eventId(),
                () -> streamPublisher.publish(StreamKey.ROOM_JOIN, event),
                "방 참가",
                String.format("joinCode=%s, guestName=%s", joinCode, guestName),
                room -> String.format("joinCode=%s, guestName=%s", joinCode, guestName)
        );
    }

    public Winner spinRoulette(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        Player host = room.findPlayer(new PlayerName(hostName));

        return room.spinRoulette(host, new Roulette(new RoulettePicker()));
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

    public boolean isReadyState(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.isReadyState();
    }

    public QrCodeStatusResponse getQrCodeStatus(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final QrCode qrCode = room.getJoinCode().getQrCode();

        QrCodeStatusResponse response = new QrCodeStatusResponse(qrCode.getStatus(), qrCode.getUrl());

        log.debug("QR 코드 상태 반환: joinCode={}, status={}", joinCode, qrCode.getStatus());
        return response;
    }

    public List<Playable> getRemainingMiniGames(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.getMiniGames().stream().toList();
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

    public void updateMiniGames(MiniGameSelectEvent event) {
        log.info("JoinCode[{}] 미니게임 목록 업데이트 이벤트 처리 - 호스트: {}, 미니게임 종류: {}",
                event.joinCode(),
                event.hostName(),
                event.miniGameTypes()
        );

        roomCommandService.updateMiniGames(
                new JoinCode(event.joinCode()),
                new PlayerName(event.hostName()),
                event.miniGameTypes()
        );

        eventPublisher.publishEvent(event);
    }

    public void readyPlayer(PlayerReadyEvent event) {
        log.info("JoinCode[{}] 플레이어 준비 상태 변경 이벤트 처리 - 플레이어: {}, 준비 상태: {}",
                event.joinCode(),
                event.playerName(),
                event.isReady()
        );

        roomCommandService.readyPlayer(new JoinCode(event.joinCode()),
                new PlayerName(event.playerName()),
                event.isReady()
        );

        eventPublisher.publishEvent(new PlayerListUpdateEvent(event.joinCode()));
    }

    public void handleQrCodeStatus(QrCodeStatusEvent event) {
        log.info(
                "QR 코드 완료 이벤트 수신: eventId={}, joinCode={}, status={}",
                event.eventId(), event.joinCode(), event.status()
        );

        switch (event.status()) {
            case SUCCESS -> {
                log.info(
                        "QR 코드 완료 이벤트 처리 완료 (SUCCESS): eventId={}, joinCode={}, url={}",
                        event.eventId(), event.joinCode(), event.qrCodeUrl()
                );
                roomCommandService.assignQrCode(new JoinCode(event.joinCode()), event.qrCodeUrl());
                eventPublisher.publishEvent(event);
            }
            case ERROR -> {
                log.info(
                        "QR 코드 완료 이벤트 처리 완료 (ERROR): eventId={}, joinCode={}",
                        event.eventId(), event.joinCode()
                );
                roomCommandService.assignQrCodeError(new JoinCode(event.joinCode()));
                eventPublisher.publishEvent(event);
            }
            default -> log.error(
                    "처리할 수 없는 QR 코드 상태: eventId={}, joinCode={}, status={}",
                    event.eventId(), event.joinCode(), event.status()
            );
        }
    }

    private void saveRoomEntity(String joinCodeValue) {
        final RoomEntity roomEntity = new RoomEntity(joinCodeValue);
        roomJpaRepository.save(roomEntity);
    }
}
