package coffeeshout.profanity.infra;

import coffeeshout.profanity.application.ProfanityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NicknameAuditScheduler {

    private final ProfanityAuditService profanityAuditService;

    @Scheduled(cron = "0 0 0/12 * * *")
    public void auditPendingNicknames() {
        log.info("닉네임 AI 검열 스케줄러 시작");
        profanityAuditService.auditPending();
    }
}
