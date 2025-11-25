package coffeeshout.room.application;

import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.Winner;
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

    private final RoomJpaRepository roomJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;
    private final RouletteResultJpaRepository rouletteResultJpaRepository;

    @Transactional
    public void updateRoomStatusToRoulette(String joinCode) {
        final RoomEntity roomEntity = getRoomEntity(joinCode);
        roomEntity.updateRoomStatus(RoomState.ROULETTE);

        log.info("RoomEntity 상태 업데이트 완료: joinCode={}, status=ROULETTE", joinCode);
    }

    @Transactional
    public void saveRouletteResult(String joinCode, Winner winner) {
        // RoomEntity 조회 및 상태 업데이트
        final RoomEntity roomEntity = getRoomEntity(joinCode);
        roomEntity.updateRoomStatus(RoomState.DONE);
        roomEntity.finish();

        // PlayerEntity 조회
        final PlayerEntity playerEntity = getPlayerEntity(roomEntity, winner.name().value());

        // RouletteResultEntity 저장
        final RouletteResultEntity rouletteResult = new RouletteResultEntity(
                roomEntity,
                playerEntity,
                winner.probability()
        );
        rouletteResultJpaRepository.save(rouletteResult);

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
