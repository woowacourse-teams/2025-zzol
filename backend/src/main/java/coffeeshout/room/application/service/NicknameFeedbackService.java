package coffeeshout.room.application.service;

import coffeeshout.room.infra.persistence.CustomProfanityEntity;
import coffeeshout.room.infra.persistence.CustomProfanityJpaRepository;
import coffeeshout.room.infra.persistence.NicknameAuditEntity;
import coffeeshout.room.infra.persistence.NicknameAuditJpaRepository;
import coffeeshout.room.infra.persistence.NicknameFeedbackEntity;
import coffeeshout.room.infra.persistence.NicknameFeedbackJpaRepository;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
    public void approve(Long auditId) {
        NicknameAuditEntity audit = getAuditEntity(auditId);
        feedbackRepository.save(new NicknameFeedbackEntity(
                audit.getNickname(),
                true,
                audit.getConfidence(),
                NicknameFeedbackEntity.OperatorDecision.APPROVED,
                null
        ));
        log.info("닉네임 검열 승인 처리: auditId={}, nickname={}", auditId, audit.getNickname());
    }

    @Transactional
    public void reject(Long auditId) {
        NicknameAuditEntity audit = getAuditEntity(auditId);
        String nickname = audit.getNickname();

        feedbackRepository.save(new NicknameFeedbackEntity(
                nickname,
                true,
                audit.getConfidence(),
                NicknameFeedbackEntity.OperatorDecision.REJECTED,
                null
        ));

        if (!customProfanityRepository.existsByWord(nickname)) {
            customProfanityRepository.save(
                    new CustomProfanityEntity(nickname, CustomProfanityEntity.Source.AI_AUDIT)
            );
            badWordFiltering.add(nickname);
            log.info("커스텀 비속어 등록: nickname={}", nickname);
        }

        log.info("닉네임 검열 거절 처리: auditId={}, nickname={}", auditId, nickname);
    }

    private NicknameAuditEntity getAuditEntity(Long auditId) {
        return auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("검열 항목을 찾을 수 없습니다: " + auditId));
    }
}
