package coffeeshout.profanity.infra;

import coffeeshout.profanity.application.ProfanityAuditService;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 12시간 주기 닉네임 AI 검열 트리거.
 *
 * <p>검열 작업을 직접 실행하지 않고 전용 실행기로 넘긴다. 프로덕션에서 모든 {@code @Scheduled}가
 * 단일 스레드를 공유하는데(그 안에 500ms 주기 Outbox 릴레이가 있다), 검열 회차는 적체량에 비례해
 * 분에서 시간 단위로 길어지기 때문이다. 여기서 직접 돌리면 그동안 다른 스케줄 작업이 전부 굶는다.
 *
 * <p>실행기가 공유 {@code virtualThreadExecutor}가 아니라 전용 빈인 이유는
 * {@code NicknameAuditConfig#nicknameAuditExecutor()} 주석에 있다. 종료 시 회차를 끊어야 해서다.
 *
 * <p>중복 실행 방지는 {@link ProfanityAuditService#auditPending()}의 분산 락이 담당한다.
 * 이 메서드에 락을 걸면 넘기자마자 반환하면서 락도 풀려 아무것도 지키지 못한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NicknameAuditScheduler {

    private final ProfanityAuditService profanityAuditService;

    @Qualifier("nicknameAuditExecutor")
    private final Executor auditExecutor;

    @Scheduled(cron = "0 0 0/12 * * *")
    public void auditPendingNicknames() {
        log.info("닉네임 AI 검열 스케줄러 시작 — 실행기로 넘긴다");
        auditExecutor.execute(this::runAudit);
    }

    private void runAudit() {
        try {
            profanityAuditService.auditPending();
        } catch (Exception e) {
            // 실행기 스레드에서 예외가 새면 원인이 로그에 안 남는다.
            log.error("닉네임 AI 검열 실패", e);
        }
    }
}
