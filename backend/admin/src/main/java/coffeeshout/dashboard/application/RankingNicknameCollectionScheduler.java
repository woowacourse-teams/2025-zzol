package coffeeshout.dashboard.application;

import coffeeshout.dashboard.domain.RacingGameTopPlayerResponse;
import coffeeshout.dashboard.domain.TopWinnerResponse;
import coffeeshout.dashboard.domain.repository.DashboardStatisticsRepository;
import coffeeshout.global.nickname.NicknamesCollectedEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingNicknameCollectionScheduler {

    private static final int RANKING_LIMIT = 50;

    private final Clock clock;
    private final DashboardStatisticsRepository dashboardRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 30 0/12 * * *")
    public void collectAndPublish() {
        log.info("랭킹 닉네임 수집 스케줄러 시작");
        final long start = System.currentTimeMillis();
        try {
            final LocalDateTime now = LocalDateTime.now(clock);
            final LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

            final Set<String> nicknames = collectRankingNicknames(startOfMonth, now);
            eventPublisher.publishEvent(new NicknamesCollectedEvent(nicknames));

            log.info("랭킹 닉네임 수집 완료 ({}건, {}ms)", nicknames.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("랭킹 닉네임 수집 스케줄러 실패 ({}ms)", System.currentTimeMillis() - start, e);
        }
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
}
