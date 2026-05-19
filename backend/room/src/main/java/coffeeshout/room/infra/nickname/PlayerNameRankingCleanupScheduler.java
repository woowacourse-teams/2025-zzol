package coffeeshout.room.infra.nickname;

import coffeeshout.room.application.service.player.name.PlayerNameRankingCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerNameRankingCleanupScheduler {

    private final PlayerNameRankingCleanupService playerNameRankingCleanupService;

    @Scheduled(cron = "0 30 0/12 * * *")
    public void cleanupBlockedNicknames() {
        log.info("랭킹 닉네임 정제 스케줄러 시작");
        final long start = System.currentTimeMillis();
        try {
            playerNameRankingCleanupService.cleanupBlockedNicknames();
            log.info("랭킹 닉네임 정제 스케줄러 완료 ({}ms)", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("랭킹 닉네임 정제 스케줄러 실패 ({}ms)", System.currentTimeMillis() - start, e);
        }
    }
}
