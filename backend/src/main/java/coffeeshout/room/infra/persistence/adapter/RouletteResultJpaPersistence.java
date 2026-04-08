package coffeeshout.room.infra.persistence.adapter;

import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.Winner;
import coffeeshout.room.domain.repository.RouletteResultPersistence;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import coffeeshout.room.infra.persistence.RouletteResultEntity;
import coffeeshout.room.infra.persistence.RouletteResultJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RouletteResultJpaPersistence implements RouletteResultPersistence {

    private final RoomJpaRepository roomJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;
    private final RouletteResultJpaRepository rouletteResultJpaRepository;

    @Override
    @Transactional
    public void updateRoomStatusToRoulette(String joinCode) {
        final RoomEntity roomEntity = getRoomEntity(joinCode);
        roomEntity.updateRoomStatus(RoomState.ROULETTE);

        log.info("RoomEntity 상태 업데이트 완료: joinCode={}, status=ROULETTE", joinCode);
    }

    @Override
    @Transactional
    public void finishRoomAndSaveResult(String joinCode, Winner winner) {
        final RoomEntity roomEntity = getRoomEntity(joinCode);

        if (roomEntity.isDone()) {
            log.info("이미 처리된 룰렛 결과입니다. (중복 저장 방지): joinCode={}", joinCode);
            return;
        }

        roomEntity.finish();
        roomJpaRepository.saveAndFlush(roomEntity);

        final PlayerEntity playerEntity = getPlayerEntity(roomEntity, winner.name().value());
        rouletteResultJpaRepository.save(new RouletteResultEntity(roomEntity, playerEntity, winner.probability()));

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
