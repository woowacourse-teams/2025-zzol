package coffeeshout.room.infra;

import coffeeshout.room.application.service.nickname.PlayerNameRankingCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerNameRankingCleanupScheduler {

    private final PlayerNameRankingCleanupService playerNameRankingCleanupService;

    @Scheduled(cron = "0 0 0/12 * * *")
    public void cleanupBlockedNicknames() {
        log.info("랭킹 닉네임 정제 스케줄러 시작");
        playerNameRankingCleanupService.cleanupBlockedNicknames();
    }
}
