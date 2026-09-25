package coffeeshout.profanity.application;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.application.port.NicknameFeedbackRepository;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
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
        allow(getAuditEntity(auditId));
    }

    @Transactional
    public void block(Long auditId) {
        block(getAuditEntity(auditId));
    }

    /**
     * 표본을 정상으로 확정한다. 허용과 같은 경로를 타고, 표본 표시가 남아 있어 품질 집계는 판정 일치로 센다.
     *
     * <p>검토 대기 중인 표본만 받는다. FLAGGED 행 id가 넘어오면 허용 경로를 타 오탐으로 세어지고,
     * 이미 결정한 표본이 다시 넘어오면 피드백이 두 번 쌓인다.
     */
    @Transactional
    public void allowSample(Long auditId) {
        allow(getUnreviewedSample(auditId));
    }

    /** 표본을 미탐으로 확정한다. 차단과 같은 경로를 타 사전에 올리고, 품질 집계는 미탐으로 센다. */
    @Transactional
    public void blockSample(Long auditId) {
        block(getUnreviewedSample(auditId));
    }

    private void allow(NicknameAudit audit) {
        final String nickname = audit.getNickname();
        audit.updateStatus(NicknameAuditStatus.ALLOWED);
        feedbackRepository.save(
                new NicknameFeedback(nickname, audit.getConfidence(), NicknameFeedback.OperatorDecision.ALLOWED, null));
        profanityWordManagementService.operatorAllow(nickname);
        log.info("닉네임 허용 처리: auditId={}, nickname={}", audit.getId(), nickname);
    }

    private void block(NicknameAudit audit) {
        final String nickname = audit.getNickname();
        audit.updateStatus(NicknameAuditStatus.BLOCKED);
        feedbackRepository.save(
                new NicknameFeedback(nickname, audit.getConfidence(), NicknameFeedback.OperatorDecision.BLOCKED, null));
        if (profanityWordManagementService.add(nickname, Language.detect(nickname), WordSource.MANUAL)) {
            eventPublisher.publishEvent(new ProfanityWordBlockedEvent(nickname));
        }
        log.info("닉네임 차단 처리: auditId={}, nickname={}", audit.getId(), nickname);
    }

    private NicknameAudit getUnreviewedSample(Long auditId) {
        final NicknameAudit audit = getAuditEntity(auditId);
        if (!audit.isUnreviewedSample()) {
            throw new BusinessException(NicknameAuditErrorCode.NOT_UNREVIEWED_SAMPLE, "검토를 기다리는 표본이 아닙니다: " + auditId);
        }
        return audit;
    }

    private NicknameAudit getAuditEntity(Long auditId) {
        return auditRepository
                .findById(auditId)
                .orElseThrow(() ->
                        new BusinessException(NicknameAuditErrorCode.AUDIT_NOT_FOUND, "검열 항목을 찾을 수 없습니다: " + auditId));
    }
}
