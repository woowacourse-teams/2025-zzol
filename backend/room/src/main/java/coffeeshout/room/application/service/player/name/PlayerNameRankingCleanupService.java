package coffeeshout.room.application.service.player.name;

import coffeeshout.global.nickname.NicknamesCollectedEvent;
import coffeeshout.global.nickname.ProfanityChecker;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.PlayerNameGenerator;
import coffeeshout.room.application.port.PlayerEntityRepository;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.RoomEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerNameRankingCleanupService {

    private final ProfanityChecker profanityChecker;
    private final PlayerEntityRepository playerRepository;
    private final PlayerNameGenerator nicknameGenerator;

    @EventListener
    @Transactional
    public void onNicknamesCollected(NicknamesCollectedEvent event) {
        final List<String> originalTargets = event.nicknames().stream()
                .filter(profanityChecker::contains)
                .toList();

        if (originalTargets.isEmpty()) {
            log.info("[RankingCleanup] 랭킹 내 BLOCKED 닉네임 없음, 종료");
            return;
        }

        log.info("[RankingCleanup] 교체 대상 {}건: {}", originalTargets.size(), originalTargets);
        int replaced = 0;
        for (final String nickname : originalTargets) {
            replaced += replaceNickname(nickname);
        }
        log.info("[RankingCleanup] 닉네임 교체 완료: 총 {}건", replaced);
    }

    private int replaceNickname(String nickname) {
        final List<PlayerEntity> targets = playerRepository.findAllByPlayerName(nickname);
        if (targets.isEmpty()) return 0;

        final List<RoomEntity> rooms = targets.stream()
                .map(PlayerEntity::getRoomSession)
                .distinct()
                .toList();

        final Map<RoomEntity, Set<String>> namesByRoom = playerRepository.findAllByRoomSessionIn(rooms)
                .stream()
                .collect(Collectors.groupingBy(
                        PlayerEntity::getRoomSession,
                        Collectors.mapping(PlayerEntity::getPlayerName, Collectors.toSet())
                ));

        for (final PlayerEntity player : targets) {
            final Set<String> existingNamesInRoom = namesByRoom.getOrDefault(player.getRoomSession(), Set.of());
            final PlayerName newName = nicknameGenerator.generate(existingNamesInRoom);
            log.debug("[RankingCleanup] {} → {} (playerId={})", nickname, newName, player.getId());
            player.updatePlayerName(newName);
        }
        return targets.size();
    }
}
