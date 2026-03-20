package coffeeshout.room.application.service.nickname;

import coffeeshout.room.domain.audit.NicknameAuditStatus;
import coffeeshout.room.infra.persistence.CustomProfanityEntity;
import coffeeshout.room.infra.persistence.CustomProfanityJpaRepository;
import coffeeshout.room.infra.persistence.NicknameAuditEntity;
import coffeeshout.room.infra.persistence.NicknameAuditJpaRepository;
import coffeeshout.room.infra.persistence.NicknameFeedbackEntity;
import coffeeshout.room.infra.persistence.NicknameFeedbackJpaRepository;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NicknameFeedbackService {

    private final NicknameAuditJpaRepository auditRepository;
    private final NicknameFeedbackJpaRepository feedbackRepository;
    private final CustomProfanityJpaRepository customProfanityRepository;
    private final BadWordFiltering badWordFiltering;

    @Transactional
    public void allow(Long auditId) {
        NicknameAuditEntity audit = getAuditEntity(auditId);
        audit.updateStatus(NicknameAuditStatus.ALLOWED);
        feedbackRepository.save(new NicknameFeedbackEntity(
                audit.getNickname(),
                true,
                audit.getConfidence(),
                NicknameFeedbackEntity.OperatorDecision.ALLOWED,
                null
        ));
        log.info("닉네임 허용 처리: auditId={}, nickname={}", auditId, audit.getNickname());
    }

    @Transactional
    public void block(Long auditId) {
        NicknameAuditEntity audit = getAuditEntity(auditId);
        String nickname = audit.getNickname();
        audit.updateStatus(NicknameAuditStatus.BLOCKED);

        feedbackRepository.save(new NicknameFeedbackEntity(
                nickname,
                true,
                audit.getConfidence(),
                NicknameFeedbackEntity.OperatorDecision.BLOCKED,
                null
        ));

        if (!customProfanityRepository.existsByWord(nickname)) {
            customProfanityRepository.save(
                    new CustomProfanityEntity(nickname, CustomProfanityEntity.Source.AI_AUDIT)
            );
            badWordFiltering.add(nickname);
            log.info("커스텀 비속어 등록: nickname={}", nickname);
        }

        log.info("닉네임 차단 처리: auditId={}, nickname={}", auditId, nickname);
    }

    private NicknameAuditEntity getAuditEntity(Long auditId) {
        return auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("검열 항목을 찾을 수 없습니다: " + auditId));
    }
}
