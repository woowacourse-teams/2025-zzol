package coffeeshout.zzolbot.monitor.application;

import coffeeshout.global.lock.RedisLock;
import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 능동 모니터링 스케줄러. 기본 4시간 주기로 점검을 시작한다.
 *
 * <p>다중 인스턴스 중복 실행은 {@link RedisLock}으로 막는다. waitTime=0이라 락을 못 잡은 인스턴스는
 * 즉시 스킵하고, doneTtl(5분)은 동시 발화 인스턴스 간 중복만 막을 만큼 짧아 다음 주기를 차단하지 않는다.
 * RedissonClient가 없으면 Aspect가 비활성화되어 단일 인스턴스로 그대로 동작한다.
 *
 * <p>{@code @Profile("!test")}로 테스트에서는 스케줄이 돌지 않는다. 실제 점검 로직은
 * {@link MonitorService#runOnce()}에 있어 어드민 수동 트리거와 공유한다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MonitorScheduler {

    private final MonitorService monitorService;
    private final MonitorProperties properties;

    @Scheduled(cron = "${zzol-bot.monitor.cron}")
    @RedisLock(
            key = "'monitor'",
            lockPrefix = "zzolbot:monitor:lock:",
            donePrefix = "zzolbot:monitor:done:",
            waitTime = 0,
            leaseTime = 300_000,
            doneTtl = 300_000
    )
    public void scan() {
        if (!properties.enabled()) {
            log.debug("[ZzolBot] 모니터링 비활성 — 스킵");
            return;
        }
        log.info("[ZzolBot] 능동 모니터링 점검 시작");
        monitorService.runOnce();
    }
}
