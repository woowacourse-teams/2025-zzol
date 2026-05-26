package coffeeshout.profanity.application;

import coffeeshout.global.event.ProfanityWordBlockedEvent;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.ProfanityErrorCode;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityFeedbackService {

    private final NicknameAuditRepository auditRepository;
    private final NicknameFeedbackRepository feedbackRepository;
    private final ProfanityWordManagementService profanityWordManagementService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void allow(Long auditId) {
        final NicknameAudit audit = getAuditEntity(auditId);
        final String nickname = audit.getNickname();
        audit.updateStatus(NicknameAuditStatus.ALLOWED);
        feedbackRepository.save(new NicknameFeedback(
                nickname,
                true,
                audit.getConfidence(),
                NicknameFeedback.OperatorDecision.ALLOWED,
                null
        ));
        tryDeactivate(nickname);
        log.info("닉네임 허용 처리: auditId={}, nickname={}", auditId, nickname);
    }

    @Transactional
    public void block(Long auditId) {
        final NicknameAudit audit = getAuditEntity(auditId);
        final String nickname = audit.getNickname();
        audit.updateStatus(NicknameAuditStatus.BLOCKED);
        feedbackRepository.save(new NicknameFeedback(
                nickname,
                true,
                audit.getConfidence(),
                NicknameFeedback.OperatorDecision.BLOCKED,
                null
        ));
        if (profanityWordManagementService.add(nickname, Language.KOREAN, WordSource.MANUAL)) {
            eventPublisher.publishEvent(new ProfanityWordBlockedEvent(nickname));
        }
        log.info("닉네임 차단 처리: auditId={}, nickname={}", auditId, nickname);
    }

    private void tryDeactivate(String nickname) {
        try {
            profanityWordManagementService.deactivate(nickname);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ProfanityErrorCode.WORD_NOT_FOUND) {
                throw e;
            }
            log.debug("비속어 목록에 없는 닉네임 허용 — 비활성화 생략: {}", nickname);
        }
    }

    private NicknameAudit getAuditEntity(Long auditId) {
        return auditRepository.findById(auditId)
                .orElseThrow(() -> new BusinessException(
                        NicknameAuditErrorCode.AUDIT_NOT_FOUND, "검열 항목을 찾을 수 없습니다: " + auditId));
    }
}
