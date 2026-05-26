package coffeeshout.room.application.service.player.name;

import coffeeshout.profanity.domain.ProfanityWord;
import coffeeshout.profanity.domain.ProfanityWordRepository;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.global.nickname.NicknamesCollectedEvent;
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

    private final ProfanityWordRepository profanityWordRepository;
    private final TextNormalizer textNormalizer;
    private final PlayerEntityRepository playerRepository;
    private final PlayerNameGenerator nicknameGenerator;

    @EventListener
    @Transactional
    public void onNicknamesCollected(NicknamesCollectedEvent event) {
        // DB 저장 정규화(TextNormalizer + normalizeWord)와 동일하게 적용해 매칭 키를 맞춘다.
        // 원본 닉네임은 이후 findAllByPlayerName 조회에 사용하므로 정규화값→원본 매핑을 보존한다.
        final Map<String, List<String>> normalizedToOriginals = event.nicknames().stream()
                .collect(Collectors.groupingBy(
                        n -> ProfanityWord.normalizeWord(textNormalizer.normalize(n))));

        final Set<String> blocked = profanityWordRepository.findAllActiveIn(normalizedToOriginals.keySet());

        if (blocked.isEmpty()) {
            log.info("[RankingCleanup] 랭킹 내 BLOCKED 닉네임 없음, 종료");
            return;
        }

        final List<String> originalTargets = blocked.stream()
                .flatMap(key -> normalizedToOriginals.getOrDefault(key, List.of()).stream())
                .toList();

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
