package coffeeshout.room.application.service;

import coffeeshout.room.application.service.nickname.NicknameAuditService;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import coffeeshout.room.infra.persistence.RouletteResultEntity;
import coffeeshout.room.infra.persistence.RouletteResultJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouletteService {

    private final RoomQueryService roomQueryService;
    private final RoomJpaRepository roomJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;
    private final RouletteResultJpaRepository rouletteResultJpaRepository;
    private final NicknameAuditService nicknameAuditService;

    public RoomState showRoulette(String joinCode) {
        final Room room = roomQueryService.getByJoinCode(new JoinCode(joinCode));
        room.showRoulette();
        return room.getRoomState();
    }

    @Transactional
    public void updateRoomStatusToRoulette(String joinCode) {
        final RoomEntity roomEntity = getRoomEntity(joinCode);
        roomEntity.updateRoomStatus(RoomState.ROULETTE);

        log.info("RoomEntity 상태 업데이트 완료: joinCode={}, status=ROULETTE", joinCode);
    }

    @Transactional
    public void saveRouletteResult(String joinCode, Winner winner) {
        final RoomEntity roomEntity = getRoomEntity(joinCode);

        if (roomEntity.isDone()) {
            log.info("이미 처리된 룰렛 결과입니다. (중복 저장 방지): joinCode={}", joinCode);
            return;
        }

        roomEntity.finish();
        roomJpaRepository.saveAndFlush(roomEntity);

        final PlayerEntity playerEntity = getPlayerEntity(roomEntity, winner.name().value());

        final RouletteResultEntity rouletteResult = new RouletteResultEntity(
                roomEntity,
                playerEntity,
                winner.probability()
        );
        rouletteResultJpaRepository.save(rouletteResult);
        nicknameAuditService.register(winner.name().value());

        log.info("RouletteResultEntity 저장 완료: joinCode={}, winner={}, probability={}",
                joinCode, winner.name().value(), winner.probability());
    }

    private RoomEntity getRoomEntity(String joinCode) {
        return roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .orElseThrow(() -> new IllegalArgumentException("RoomEntity를 찾을 수 없습니다: " + joinCode));
    }

    private PlayerEntity getPlayerEntity(RoomEntity roomEntity, String playerName) {
        return playerJpaRepository.findByRoomSessionAndPlayerName(roomEntity, playerName)
                .orElseThrow(() -> new IllegalArgumentException("PlayerEntity를 찾을 수 없습니다: " + playerName));
    }
}
