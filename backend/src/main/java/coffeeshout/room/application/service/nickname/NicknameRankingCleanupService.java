package coffeeshout.room.application.service.nickname;

import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.room.domain.audit.NicknameAuditStatus;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.PlayerJpaRepository;
import coffeeshout.room.infra.persistence.nickname.NicknameAuditJpaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NicknameRankingCleanupService {

    private static final int RANKING_LIMIT = 50;

    private final Clock clock;
    private final DashboardStatisticsRepository dashboardRepository;
    private final NicknameAuditJpaRepository auditRepository;
    private final PlayerJpaRepository playerRepository;
    private final NicknameGenerator nicknameGenerator;

    @Transactional
    public void cleanupBlockedNicknames() {
        Set<String> blockedNicknames = auditRepository.findNicknamesByStatus(NicknameAuditStatus.BLOCKED);
        if (blockedNicknames.isEmpty()) {
            log.info("[RankingCleanup] BLOCKED 닉네임 없음, 종료");
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        Set<String> rankingNicknames = collectRankingNicknames(startOfMonth, now);
        Set<String> targets = new HashSet<>(rankingNicknames);
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
        Set<String> nicknames = new HashSet<>();

        dashboardRepository.findTopWinnersBetween(start, end, RANKING_LIMIT)
                .stream()
                .map(TopWinnerResponse::playerName)
                .forEach(nicknames::add);

        dashboardRepository.findRacingGameTopPlayers(start, end, RANKING_LIMIT)
                .stream()
                .map(RacingGameTopPlayerResponse::playerName)
                .forEach(nicknames::add);

        return nicknames;
    }

    private int replaceNickname(String nickname) {
        List<PlayerEntity> players = playerRepository.findAllByPlayerName(nickname);
        for (PlayerEntity player : players) {
            Set<String> existingNamesInRoom = playerRepository.findAllByRoomSession(player.getRoomSession())
                    .stream()
                    .map(PlayerEntity::getPlayerName)
                    .collect(Collectors.toSet());

            String newName = nicknameGenerator.generate(existingNamesInRoom);
            log.debug("[RankingCleanup] {} → {} (playerId={})", nickname, newName, player.getId());
            player.updatePlayerName(newName);
        }
        return players.size();
    }
}
