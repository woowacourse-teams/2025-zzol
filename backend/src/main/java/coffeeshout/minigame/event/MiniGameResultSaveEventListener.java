package coffeeshout.minigame.event;

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
import coffeeshout.room.domain.Playable;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.service.RoomQueryService;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import jakarta.transaction.Transactional;
import java.util.Map;
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
        final Map<Player, MiniGameScore> scores = miniGame.getScores();

        for (Player player : room.getPlayers()) {
            final PlayerEntity playerEntity = playerJpaRepository.findByRoomSessionAndPlayerName(roomEntity,
                            player.getName().value())
                    .orElseThrow(() -> new IllegalArgumentException("플레이어가 존재하지 않습니다: " + player.getName().value()));

            final Integer rank = result.getPlayerRank(player);
            final Long score = scores.get(player).getValue();

            final MiniGameResultEntity resultEntity = new MiniGameResultEntity(
                    miniGameEntity,
                    playerEntity,
                    rank,
                    score
            );

            miniGameResultJpaRepository.save(resultEntity);
        }

        log.info("미니게임 결과 저장 완료: joinCode={}", event.joinCode());
    }
}
