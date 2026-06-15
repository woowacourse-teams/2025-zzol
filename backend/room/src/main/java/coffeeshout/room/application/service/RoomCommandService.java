package coffeeshout.room.application.service;

import coffeeshout.gamecommon.RoomLifecycleEvent;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.domain.QrCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.player.PlayerType;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class RoomCommandService {

    private final RoomRepository roomRepository;
    private final RoomQueryService roomQueryService;
    private final StreamPublisher streamPublisher;

    public Room save(Room room) {
        return roomRepository.save(room);
    }

    /**
     * 게임 종료 결과(순위 맵·라운드 수)로 확률을 조정한다. {@code :game}의
     * {@code MiniGameFinishedEvent}를 수신한 {@code MiniGameResultRoomListener}가 호출한다(ADR-0025 결정 5).
     */
    public void applyGameResult(JoinCode joinCode, Map<PlayerName, Integer> rankByPlayer, int roundCount) {
        final Room room = roomQueryService.getByJoinCode(joinCode);
        room.applyGameResult(rankByPlayer, roundCount);
        save(room);
    }

    public void delete(@NonNull JoinCode joinCode) {
        roomRepository.deleteByJoinCode(joinCode);
    }

    public boolean removePlayer(JoinCode joinCode, PlayerName playerName) {
        log.info("JoinCode[{}] 플레이어 퇴장 - 플레이어 이름: {} ", joinCode, playerName);
        final Room room = roomQueryService.getByJoinCode(joinCode);
        final PlayerName previousHost = room.getHost().getName();

        boolean removed = room.removePlayer(playerName);

        if (removed && room.isEmpty()) {
            delete(joinCode);
            return true;
        }

        if (removed) {
            save(room);
            publishHostChangeIfPromoted(joinCode, previousHost, room);
        }

        return removed;
    }

    /**
     * 호스트가 떠나 새 호스트가 승계됐으면({@code promoteNewHost}) GameSession이 새 호스트로 갱신되도록
     * 생명주기 이벤트를 발행한다. 세션은 인스턴스 로컬이라 in-process가 아닌 Stream으로 발행해야
     * 세션을 소유한 모든 인스턴스에 도달한다(ADR-0025 결정 6, {@code RoomLifecycleEvent.Removed}와 동일 경로).
     */
    private void publishHostChangeIfPromoted(JoinCode joinCode, PlayerName previousHost, Room room) {
        final PlayerName currentHost = room.getHost().getName();
        if (!currentHost.equals(previousHost)) {
            streamPublisher.publish(
                    RoomStreamKey.BROADCAST,
                    new RoomLifecycleEvent.HostChanged(joinCode.getValue(), currentHost.value()));
        }
    }

    public Room joinGuest(JoinCode joinCode, PlayerName playerName) {
        return joinGuest(joinCode, playerName, null);
    }

    public Room joinGuest(JoinCode joinCode, PlayerName playerName, Long userId) {
        log.info("JoinCode[{}] 게스트 입장 - 게스트 이름: {} ", joinCode, playerName);
        final Room room = roomQueryService.getByJoinCode(joinCode);

        room.joinGuest(playerName, userId);

        return save(room);
    }

    public Room saveIfAbsentRoom(JoinCode joinCode, PlayerName hostName, double adjustmentWeight) {
        return saveIfAbsentRoom(joinCode, hostName, null, adjustmentWeight);
    }

    public Room saveIfAbsentRoom(JoinCode joinCode, PlayerName hostName, Long userId, double adjustmentWeight) {
        if (roomRepository.existsByJoinCode(joinCode)) {
            log.warn("JoinCode[{}] 방 생성 실패 - 이미 존재하는 방", joinCode);
            return roomQueryService.getByJoinCode(joinCode);
        }

        log.info("JoinCode[{}] 방 생성 - 호스트 이름: {} ", joinCode, hostName);

        final Room room = Room.createNewRoom(joinCode, hostName, userId, adjustmentWeight);

        return save(room);
    }

    public void updateAdjustmentWeight(JoinCode joinCode, PlayerName hostName, double adjustmentWeight) {
        log.info("JoinCode[{}] 조정 가중치 변경 - 호스트: {}, 가중치: {}", joinCode, hostName, adjustmentWeight);

        final Room room = roomQueryService.getByJoinCode(joinCode);
        room.updateAdjustmentWeight(hostName, adjustmentWeight);
        save(room);
    }

    public void assignQrCode(JoinCode joinCode, String qrCodeUrl) {
        // 비동기 QR 생성(@Async)이 끝나기 전에 방이 제거되면(사용자 이탈·TTL, 테스트 격리 정리 등) QR을 붙일 대상이 없다.
        // 예외를 던지면 스트림 컨슈머가 거대한 data-URL 페이로드와 스택을 반복 로깅해 처리량을 잠식하므로,
        // 사라진 방의 QR 이벤트는 멱등하게 무시한다(늦게 도착한 이벤트에 대한 정상적인 처리).
        if (!roomRepository.existsByJoinCode(joinCode)) {
            log.debug("이미 제거된 방의 QR SUCCESS 이벤트 무시: joinCode={}", joinCode);
            return;
        }
        final Room room = roomQueryService.getByJoinCode(joinCode);
        final QrCode currentQrCode = room.getQrCode();

        // 이미 SUCCESS 상태이고 동일한 URL이면 중복 처리 방지 (멱등성)
        if (currentQrCode.isSuccess() && qrCodeUrl.equals(currentQrCode.getUrl())) {
            log.info("이미 동일한 QR 코드가 SUCCESS 상태입니다. 무시: joinCode={}, url={}", joinCode, qrCodeUrl);
            return;
        }

        // 이미 SUCCESS 상태지만 다른 URL이면 경고 로그 (일반적으로 발생하지 않아야 함)
        if (currentQrCode.isSuccess()) {
            log.warn("이미 SUCCESS 상태인데 다른 URL로 변경 시도. 무시: joinCode={}, currentUrl={}, newUrl={}",
                    joinCode, currentQrCode.getUrl(), qrCodeUrl);
            return;
        }

        room.assignQrCode(QrCode.success(qrCodeUrl));
        save(room);
        log.info("QR 코드 SUCCESS 상태로 변경: joinCode={}, url={}", joinCode, qrCodeUrl);
    }

    public void assignQrCodeError(JoinCode joinCode) {
        if (!roomRepository.existsByJoinCode(joinCode)) {
            log.debug("이미 제거된 방의 QR ERROR 이벤트 무시: joinCode={}", joinCode);
            return;
        }
        final Room room = roomQueryService.getByJoinCode(joinCode);
        final QrCode currentQrCode = room.getQrCode();

        // 이미 SUCCESS 상태면 ERROR로 다운그레이드 방지
        if (currentQrCode.isSuccess()) {
            log.warn("이미 SUCCESS 상태이므로 ERROR 무시: joinCode={}, successUrl={}", joinCode, currentQrCode.getUrl());
            return;
        }

        // 이미 ERROR 상태면 중복 처리 방지 (멱등성)
        if (currentQrCode.isError()) {
            log.info("이미 ERROR 상태입니다. 무시: joinCode={}", joinCode);
            return;
        }

        room.assignQrCode(QrCode.error());
        save(room);
        log.info("QR 코드 ERROR 상태로 변경: joinCode={}", joinCode);
    }

    public Room readyPlayer(JoinCode joinCode, PlayerName playerName, Boolean isReady) {
        log.info("JoinCode[{}] 플레이어 준비 상태 변경 - 플레이어 이름: {}, 준비 상태: {}", joinCode, playerName, isReady);
        final Room room = roomQueryService.getByJoinCode(joinCode);
        final Player player = room.findPlayer(playerName);

        if (player.getPlayerType() == PlayerType.HOST) {
            return room;
        }

        player.updateReadyState(isReady);

        return save(room);
    }
}
