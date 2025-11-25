package coffeeshout.room.domain.service;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.QrCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.menu.Menu;
import coffeeshout.room.domain.menu.MenuTemperature;
import coffeeshout.room.domain.menu.SelectedMenu;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
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

    public Room save(Room room) {
        return roomRepository.save(room);
    }

    public void delete(@NonNull JoinCode joinCode) {
        roomRepository.deleteByJoinCode(joinCode);
    }

    public Room joinGuest(JoinCode joinCode, PlayerName playerName, Menu menu, MenuTemperature menuTemperature) {
        log.info("JoinCode[{}] 게스트 입장 - 게스트 이름: {}, 메뉴 정보: {}, 온도 : {} ", joinCode, playerName, menu, menuTemperature);
        final Room room = roomQueryService.getByJoinCode(joinCode);

        room.joinGuest(playerName, new SelectedMenu(menu, menuTemperature));

        return save(room);
    }

    public Room saveIfAbsentRoom(JoinCode joinCode, PlayerName hostName, Menu menu, MenuTemperature menuTemperature) {
        if (roomRepository.existsByJoinCode(joinCode)) {
            log.warn("JoinCode[{}] 방 생성 실패 - 이미 존재하는 방", joinCode);
            return roomQueryService.getByJoinCode(joinCode);
        }

        log.info("JoinCode[{}] 방 생성 - 호스트 이름: {}, 메뉴 정보: {}, 온도 : {} ", joinCode, hostName, menu, menuTemperature);

        final Room room = Room.createNewRoom(joinCode, hostName, new SelectedMenu(menu, menuTemperature));

        return save(room);
    }

    public void assignQrCode(JoinCode joinCode, String qrCodeUrl) {
        final Room room = roomQueryService.getByJoinCode(joinCode);
        final QrCode currentQrCode = room.getJoinCode().getQrCode();

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
        final Room room = roomQueryService.getByJoinCode(joinCode);
        final QrCode currentQrCode = room.getJoinCode().getQrCode();

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
}
