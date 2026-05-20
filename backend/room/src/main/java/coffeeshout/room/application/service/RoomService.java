package coffeeshout.room.application.service;

import coffeeshout.outbox.OutboxEventRecorder;
import coffeeshout.redis.BaseEvent;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.redis.stream.StreamPublisher;
import coffeeshout.websocket.auth.RoomSessionTokenService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.QrCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import coffeeshout.room.domain.event.PlayerReadyEvent;
import coffeeshout.room.domain.event.QrCodeStatusEvent;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.event.RouletteShowEvent;
import coffeeshout.room.domain.event.RouletteShownEvent;
import coffeeshout.room.domain.event.RouletteSpinEvent;
import coffeeshout.room.domain.event.RouletteWinnerEvent;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.roulette.Roulette;
import coffeeshout.room.domain.roulette.RoulettePicker;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.room.domain.service.PlayerNameGenerator;
import coffeeshout.room.domain.service.PlayerNameValidator;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.config.RouletteProperties;
import coffeeshout.room.infra.messaging.RoomEventWaitManager;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import coffeeshout.room.infra.persistence.RoulettePersistenceService;
import coffeeshout.room.ui.response.ProbabilityResponse;
import coffeeshout.room.ui.response.QrCodeStatusResponse;
import coffeeshout.user.application.service.UserProfileService;
import coffeeshout.user.domain.AuthenticatedUser;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    private final RouletteProperties rouletteProperties;
    private final RoomQueryService roomQueryService;
    private final RoomCommandService roomCommandService;
    private final DelayedRoomRemovalService delayedRoomRemovalService;
    private final QrCodeService qrCodeService;
    private final PlayerNameValidator playerNameValidator;
    private final PlayerNameGenerator playerNameGenerator;
    private final JoinCodeGenerator joinCodeGenerator;
    private final RoomEventWaitManager roomEventWaitManager;
    private final RoomJpaRepository roomJpaRepository;
    private final RoulettePersistenceService roulettePersistenceService;
    private final RouletteService rouletteService;
    private final OutboxEventRecorder outboxEventRecorder;
    private final StreamPublisher streamPublisher;
    private final ApplicationEventPublisher eventPublisher;
    private final UserProfileService userProfileService;
    private final RoomSessionTokenService roomSessionTokenService;

    @Value("${room.event.timeout:PT5S}")
    private Duration eventTimeout;

    @Transactional
    public RoomCreateResult createRoom(String hostName) {
        playerNameValidator.validate(new PlayerName(hostName));
        final Room room = doCreateRoom(hostName, null);
        final String token = roomSessionTokenService.issue(room.getJoinCode().getValue(), hostName, null);
        return new RoomCreateResult(room, token);
    }

    @Transactional
    public RoomCreateResult createRoom(AuthenticatedUser authUser) {
        final String nickname = userProfileService.findById(authUser.userId()).getNickname().value();
        final Room room = doCreateRoom(nickname, authUser.userId());
        final String token = roomSessionTokenService.issue(room.getJoinCode().getValue(), nickname, authUser.userId());
        return new RoomCreateResult(room, token);
    }

    private Room doCreateRoom(String resolvedName, Long userId) {
        final JoinCode joinCode = joinCodeGenerator.generate();
        final Room room = roomCommandService.saveIfAbsentRoom(joinCode, new PlayerName(resolvedName), userId, rouletteProperties.defaultAdjustmentWeight());
        final BaseEvent event = new RoomCreateEvent(resolvedName, joinCode.getValue());

        outboxEventRecorder.record(RoomStreamKey.BROADCAST, event);
        qrCodeService.generateQrCodeAsync(joinCode.getValue());
        saveRoomEntity(joinCode.getValue());

        log.info("방 생성 이벤트 처리 완료 (DB 저장): eventId={}, joinCode={}, hostName={}",
                event.eventId(), joinCode.getValue(), resolvedName);
        return room;
    }

    public CompletableFuture<RoomEnterResult> enterRoomAsync(String joinCode, String guestName) {
        playerNameValidator.validate(new PlayerName(guestName));
        return doEnterRoomAsync(joinCode, guestName, null)
                .thenApply(room -> {
                    final String token = roomSessionTokenService.issue(joinCode, guestName, null);
                    return new RoomEnterResult(room, guestName, token);
                });
    }

    public CompletableFuture<RoomEnterResult> enterRoomAsync(String joinCode, AuthenticatedUser authUser) {
        final String nickname = userProfileService.findById(authUser.userId()).getNickname().value();
        return doEnterRoomAsync(joinCode, nickname, authUser.userId())
                .thenApply(room -> {
                    final String token = roomSessionTokenService.issue(joinCode, nickname, authUser.userId());
                    return new RoomEnterResult(room, nickname, token);
                });
    }

    private CompletableFuture<Room> doEnterRoomAsync(String joinCode, String resolvedName, Long userId) {
        final RoomJoinEvent event = new RoomJoinEvent(joinCode, resolvedName, userId);
        return processEventAsync(
                event.eventId(),
                () -> streamPublisher.publish(RoomStreamKey.JOIN, event),
                "방 참가",
                String.format("joinCode=%s, playerName=%s", joinCode, resolvedName),
                room -> String.format("joinCode=%s, playerName=%s", joinCode, resolvedName)
        );
    }

    public Winner spinRoulette(String joinCode, String hostName) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final Player host = room.findPlayer(new PlayerName(hostName));

        return room.spinRoulette(host, new Roulette(new RoulettePicker()));
    }

    public String generateRandomNicknameForGuest(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final Set<String> existingNames = room.getPlayers().stream()
                .map(player -> player.getName().value())
                .collect(Collectors.toSet());
        return playerNameGenerator.generate(existingNames).value();
    }

    public String generateRandomNicknameForHost() {
        return playerNameGenerator.generate(Set.of()).value();
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

    public void updateAdjustmentWeight(String joinCode, String hostName, double adjustmentWeight) {
        roomCommandService.updateAdjustmentWeight(
                new JoinCode(joinCode),
                new PlayerName(hostName),
                adjustmentWeight
        );
    }

    public boolean isReadyState(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        return room.isReadyState();
    }

    public QrCodeStatusResponse getQrCodeStatus(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        final QrCode qrCode = room.getJoinCode().getQrCode();

        final QrCodeStatusResponse response = new QrCodeStatusResponse(qrCode.getStatus(), qrCode.getUrl());

        log.debug("QR 코드 상태 반환: joinCode={}, status={}", joinCode, qrCode.getStatus());
        return response;
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

    public void createRoom(RoomCreateEvent event) {
        log.info("JoinCode[{}] 방 생성 이벤트 처리 - 호스트 이름: {}",
                event.joinCode(),
                event.hostName()
        );

        roomCommandService.saveIfAbsentRoom(
                new JoinCode(event.joinCode()),
                new PlayerName(event.hostName()),
                rouletteProperties.defaultAdjustmentWeight()
        );

        delayedRoomRemovalService.scheduleRemoveRoom(new JoinCode(event.joinCode()));
    }

    public void joinRoom(RoomJoinEvent event) {
        log.info("JoinCode[{}] 게스트 방 입장 이벤트 처리 - 게스트 이름: {}",
                event.joinCode(),
                event.guestName()
        );

        try {
            final Room room = roomCommandService.joinGuest(
                    new JoinCode(event.joinCode()),
                    new PlayerName(event.guestName()),
                    event.userId()
            );

            roomEventWaitManager.notifySuccess(event.eventId(), room);
        } catch (Exception e) {
            roomEventWaitManager.notifyFailure(event.eventId(), e);
            throw e;
        }
    }

    public void showRoulette(RouletteShowEvent event) {
        log.info("JoinCode[{}] 룰렛 화면 표시 이벤트 처리", event.joinCode());

        final RoomState roomState = rouletteService.showRoulette(event.joinCode());

        roulettePersistenceService.saveRoomStatus(event);

        eventPublisher.publishEvent(new RouletteShownEvent(event.joinCode(), roomState));
    }

    public void spinRoulette(RouletteSpinEvent event) {
        log.info("JoinCode[{}] 룰렛 스핀 이벤트 처리 - 당첨자: {}", event.joinCode(), event.winner().name().value());

        final Winner winner = event.winner();
        roulettePersistenceService.saveRouletteResult(event);

        eventPublisher.publishEvent(new RouletteWinnerEvent(event.joinCode(), winner));
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

    private void saveRoomEntity(String joinCodeValue) {
        final RoomEntity roomEntity = new RoomEntity(joinCodeValue);
        roomJpaRepository.save(roomEntity);
    }
}
