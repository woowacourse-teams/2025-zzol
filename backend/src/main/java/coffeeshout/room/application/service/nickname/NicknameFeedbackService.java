package coffeeshout.room.application.service.nickname;

import coffeeshout.room.domain.audit.NicknameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityEntity;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityJpaRepository;
import coffeeshout.room.infra.persistence.nickname.NicknameAuditEntity;
import coffeeshout.room.infra.persistence.nickname.NicknameAuditJpaRepository;
import coffeeshout.room.infra.persistence.nickname.NicknameFeedbackEntity;
import coffeeshout.room.infra.persistence.nickname.NicknameFeedbackJpaRepository;
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
        final NicknameAuditEntity audit = getAuditEntity(auditId);
        final String nickname = audit.getNickname();
        audit.updateStatus(NicknameAuditStatus.ALLOWED);
        feedbackRepository.save(new NicknameFeedbackEntity(
                nickname,
                true,
                audit.getConfidence(),
                NicknameFeedbackEntity.OperatorDecision.ALLOWED,
                null
        ));
        customProfanityRepository.deleteByWord(nickname);
        badWordFiltering.remove(nickname);
        log.info("닉네임 허용 처리: auditId={}, nickname={}", auditId, nickname);
    }

    @Transactional
    public void block(Long auditId) {
        final NicknameAuditEntity audit = getAuditEntity(auditId);
        final String nickname = audit.getNickname();
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
