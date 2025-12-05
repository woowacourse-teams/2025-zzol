package coffeeshout.room.application.service;

import coffeeshout.room.application.port.RoomEventPublisher;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.QrCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.event.RoomCreateEvent;
import coffeeshout.room.domain.event.RoomJoinEvent;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.room.domain.service.RoomCommandService;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.infra.messaging.RoomEventWaitManager;
import coffeeshout.room.infra.persistence.RoomPersistenceService;
import coffeeshout.room.ui.response.QrCodeStatusResponse;
import java.time.Duration;
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
    private final QrCodeService qrCodeService;
    private final JoinCodeGenerator joinCodeGenerator;
    private final RoomEventPublisher roomEventPublisher;
    private final RoomEventWaitManager roomEventWaitManager;
    private final RoomPersistenceService roomPersistenceService;

    @Value("${room.event.timeout:PT5S}")
    private Duration eventTimeout;

    @Transactional
    public Room createRoom(String hostName) {
        final JoinCode joinCode = joinCodeGenerator.generate();

        // 방 생성 (QR 코드는 PENDING 상태로 시작)
        final Room room = roomCommandService.saveIfAbsentRoom(joinCode, new PlayerName(hostName));

        // 방 생성 후 이벤트 전달
        final RoomCreateEvent event = new RoomCreateEvent(
                hostName,
                joinCode.getValue()
        );

        roomEventPublisher.publish(event);

        // QR 코드 비동기 생성 시작
        qrCodeService.generateQrCodeAsync(joinCode.getValue());

        roomPersistenceService.saveRoomSession(joinCode.getValue());

        log.info("방 생성 이벤트 처리 완료 (DB 저장): eventId={}, joinCode={}",
                event.eventId(), event.joinCode());

        // 해당 방 정보 수신
        return room;
    }

    public CompletableFuture<Room> enterRoomAsync(
            String joinCode,
            String guestName
    ) {
        final RoomJoinEvent event = new RoomJoinEvent(joinCode, guestName);

        return processEventAsync(
                event.eventId(),
                () -> roomEventPublisher.publish(event),
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

    public Room enterRoom(String joinCode, String guestName) {
        return roomCommandService.joinGuest(
                new JoinCode(joinCode),
                new PlayerName(guestName)
        );
    }

    public Room getRoomByJoinCode(String joinCode) {
        return roomQueryService.getByJoinCode(new JoinCode(joinCode));
    }

    public boolean roomExists(String joinCode) {
        return roomQueryService.existsByJoinCode(new JoinCode(joinCode));
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
}
