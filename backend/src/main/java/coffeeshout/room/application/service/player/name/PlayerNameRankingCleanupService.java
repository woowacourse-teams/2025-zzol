package coffeeshout.room.application.service.player.name;

import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.service.PlayerNameGenerator;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditJpaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import coffeeshout.room.infra.persistence.RoomEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerNameRankingCleanupService {

    private static final int RANKING_LIMIT = 50;

    private final Clock clock;
    private final DashboardStatisticsRepository dashboardRepository;
    private final PlayerNameAuditJpaRepository auditRepository;
    private final PlayerJpaRepository playerRepository;
    private final PlayerNameGenerator nicknameGenerator;

    @Transactional
    public void cleanupBlockedNicknames() {
        final Set<String> blockedNicknames = auditRepository.findPlayerNamesByStatus(PlayerNameAuditStatus.BLOCKED);
        if (blockedNicknames.isEmpty()) {
            log.info("[RankingCleanup] BLOCKED 닉네임 없음, 종료");
            return;
        }

        final LocalDateTime now = LocalDateTime.now(clock);
        final LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        final Set<String> rankingNicknames = collectRankingNicknames(startOfMonth, now);
        final Set<String> targets = new HashSet<>(rankingNicknames);
        targets.retainAll(blockedNicknames);

        if (targets.isEmpty()) {
            log.info("[RankingCleanup] 랭킹 내 BLOCKED 닉네임 없음, 종료");
            return;
        }

        log.info("[RankingCleanup] 교체 대상 {}건: {}", targets.size(), targets);
        int replaced = 0;
        for (String nickname : targets) {
            replaced += replaceNickname(nickname);
        }
        log.info("[RankingCleanup] 닉네임 교체 완료: 총 {}건", replaced);
    }

    private Set<String> collectRankingNicknames(LocalDateTime start, LocalDateTime end) {
        final Set<String> nicknames = new HashSet<>();

        dashboardRepository.findTopWinnersBetween(start, end, RANKING_LIMIT)
                .stream()
                .map(TopWinnerResponse::nickname)
                .forEach(nicknames::add);

        dashboardRepository.findRacingGameTopPlayers(start, end, RANKING_LIMIT)
                .stream()
                .map(RacingGameTopPlayerResponse::playerName)
                .forEach(nicknames::add);

        return nicknames;
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

        for (PlayerEntity player : targets) {
            final Set<String> existingNamesInRoom = namesByRoom.getOrDefault(player.getRoomSession(), Set.of());
            final PlayerName newName = nicknameGenerator.generate(existingNamesInRoom);
            log.debug("[RankingCleanup] {} → {} (playerId={})", nickname, newName, player.getId());
            player.updatePlayerName(newName);
        }
        return targets.size();
    }
}
