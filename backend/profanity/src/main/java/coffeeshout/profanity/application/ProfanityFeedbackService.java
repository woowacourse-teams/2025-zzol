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
     * 표본을 정상으로 확정한다. 상태만 ALLOWED로 바꾸고 표본 표시가 남아 품질 집계는 판정 일치로 센다.
     *
     * <p>허용 경로를 타지 않는다. 맞게 통과시킨 CLEAN은 교정이 아니라서, 피드백을 남기면 프롬프트에 넣는 최근
     * 예시가 "정상" 확인으로 밀려나고 운영자 허용 단어로 올리면 닉네임 전체가 사전에 들어가 트라이가 다시 빌드된다.
     */
    @Transactional
    public void allowSample(Long auditId) {
        claimSample(auditId, NicknameAuditStatus.ALLOWED);
        log.info("표본 정상 확정: auditId={}", auditId);
    }

    /**
     * 표본을 미탐으로 확정한다. AI 판정을 뒤집는 교정이라 차단과 같은 경로를 타 피드백과 사전에 남긴다.
     *
     * <p>상태는 조건부 UPDATE가 먼저 바꾼다. 그 UPDATE가 영속성 컨텍스트를 비우므로 다시 읽은 엔티티는
     * 이미 BLOCKED이고, 차단 경로가 같은 상태를 다시 넣어도 바뀌는 값이 없다.
     */
    @Transactional
    public void blockSample(Long auditId) {
        claimSample(auditId, NicknameAuditStatus.BLOCKED);
        block(getAuditEntity(auditId));
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

    /**
     * 확인과 변경을 조건부 UPDATE 한 문장으로 한다. 읽고 나서 확인하면 두 운영자가 같은 표본을 동시에 결정할 때
     * 둘 다 통과해 피드백과 사전 등록이 두 번 남는다.
     */
    private void claimSample(Long auditId, NicknameAuditStatus decision) {
        if (auditRepository.claimUnreviewedSample(auditId, decision) == 1) {
            return;
        }
        getAuditEntity(auditId);
        throw new BusinessException(NicknameAuditErrorCode.NOT_UNREVIEWED_SAMPLE, "검토를 기다리는 표본이 아닙니다: " + auditId);
    }

    private NicknameAudit getAuditEntity(Long auditId) {
        return auditRepository
                .findById(auditId)
                .orElseThrow(() ->
                        new BusinessException(NicknameAuditErrorCode.AUDIT_NOT_FOUND, "검열 항목을 찾을 수 없습니다: " + auditId));
    }
}
