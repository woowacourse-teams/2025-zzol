package coffeeshout.minigame.infra.persistence.adapter;

import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.domain.repository.MiniGameResultSavePort;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.minigame.infra.persistence.MiniGameResultEntity;
import coffeeshout.minigame.infra.persistence.MiniGameResultJpaRepository;
import coffeeshout.minigame.infra.persistence.mapper.MiniGameResultEntityMapper;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameResultSaveJpaAdapter implements MiniGameResultSavePort {

    private final RoomJpaRepository roomJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;
    private final MiniGameJpaRepository miniGameJpaRepository;
    private final MiniGameResultJpaRepository miniGameResultJpaRepository;
    private final MiniGameResultEntityMapper miniGameResultEntityMapper;

    @Override
    @Transactional
    public void saveResults(String joinCode, MiniGameType miniGameType,
                            List<Player> players, MiniGameResult result, Map<Player, MiniGameScore> scores) {
        final RoomEntity roomEntity = roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다: " + joinCode));

        final MiniGameEntity miniGameEntity = miniGameJpaRepository
                .findByRoomSessionAndMiniGameType(roomEntity, miniGameType)
                .orElseThrow(() -> new IllegalArgumentException("미니게임 엔티티가 존재하지 않습니다: " + joinCode));

        final List<String> playerNames = players.stream()
                .map(player -> player.getName().value())
                .toList();

        final Map<String, PlayerEntity> playerEntityMap = playerJpaRepository
                .findByRoomSessionAndPlayerNameIn(roomEntity, playerNames)
                .stream()
                .collect(Collectors.toMap(
                        PlayerEntity::getPlayerName,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        final List<MiniGameResultEntity> resultEntities = new ArrayList<>();

        for (Player player : players) {
            final PlayerEntity playerEntity = playerEntityMap.get(player.getName().value());
            if (playerEntity == null) {
                throw new IllegalArgumentException("플레이어가 존재하지 않습니다: " + player.getName().value());
            }

            final Integer rank = result.getPlayerRank(player);
            final Long score = scores.get(player).getValue();

            resultEntities.add(miniGameResultEntityMapper.toEntity(miniGameEntity, playerEntity, rank, score));
        }

        miniGameResultJpaRepository.bulkInsert(resultEntities);

        log.info("미니게임 결과 벌크 저장 완료: joinCode={}, playerCount={}", joinCode, resultEntities.size());
    }
}
