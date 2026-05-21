package coffeeshout.minigame.event;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import coffeeshout.global.lock.RedisLock;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.minigame.infra.persistence.MiniGameResultEntity;
import coffeeshout.minigame.infra.persistence.MiniGameResultJpaRepository;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import coffeeshout.user.application.service.UserStatsService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameResultSaveEventListener {

    private final RoomJpaRepository roomJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;
    private final MiniGameJpaRepository miniGameJpaRepository;
    private final MiniGameResultJpaRepository miniGameResultJpaRepository;
    private final RoomQueryService roomQueryService;
    private final UserStatsService userStatsService;

    @EventListener
    @Transactional
    @RedisLock(
            key = "#event.eventId()",
            lockPrefix = "minigame:result:lock:",
            donePrefix = "minigame:result:done:",
            waitTime = 0,
            leaseTime = 5000
    )
    public void handle(MiniGameFinishedEvent event) {
        final RoomEntity roomEntity = roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(event.joinCode())
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다: " + event.joinCode()));
        final MiniGameType miniGameType = MiniGameType.valueOf(event.miniGameType());

        final MiniGameEntity miniGameEntity = miniGameJpaRepository
                .findByRoomSessionAndMiniGameType(roomEntity, miniGameType)
                .orElseThrow(() -> new IllegalArgumentException("미니게임 엔티티가 존재하지 않습니다: " + event.joinCode()));

        final Room room = roomQueryService.getByJoinCode(new JoinCode(event.joinCode()));
        final Playable miniGame = room.findMiniGame(miniGameType);

        final MiniGameResult result = miniGame.getResult();
        final Map<Gamer, MiniGameScore> scores = miniGame.getScores();

        final List<String> playerNames = room.getPlayers().stream()
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

        for (Player player : room.getPlayers()) {
            final PlayerEntity playerEntity = playerEntityMap.get(player.getName().value());
            if (playerEntity == null) {
                throw new IllegalArgumentException("플레이어가 존재하지 않습니다: " + player.getName().value());
            }

            final Gamer gamer = player.toGamer();
            final Integer rank = result.getPlayerRank(gamer);
            final Long score = scores.get(gamer).getValue();

            resultEntities.add(new MiniGameResultEntity(
                    miniGameEntity,
                    playerEntity,
                    rank,
                    score
            ));
        }

        miniGameResultJpaRepository.bulkInsert(resultEntities);

        resultEntities.stream()
                .filter(entity -> entity.getPlayer().getUserId() != null)
                .forEach(entity -> userStatsService.updateStats(
                        entity.getPlayer().getUserId(),
                        entity.getRank() == 1
                ));

        log.info("미니게임 결과 벌크 저장 완료: joinCode={}, playerCount={}", event.joinCode(), resultEntities.size());
    }
}
