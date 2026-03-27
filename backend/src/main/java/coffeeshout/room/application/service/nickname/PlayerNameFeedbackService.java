package coffeeshout.room.application.service.nickname;

import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityEntity;
import coffeeshout.room.infra.persistence.nickname.CustomProfanityJpaRepository;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditEntity;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditJpaRepository;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackEntity;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackJpaRepository;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerNameFeedbackService {

    private final PlayerNameAuditJpaRepository auditRepository;
    private final PlayerNameFeedbackJpaRepository feedbackRepository;
    private final CustomProfanityJpaRepository customProfanityRepository;
    private final BadWordFiltering badWordFiltering;

    @Transactional
    public void allow(Long auditId) {
        final PlayerNameAuditEntity audit = getAuditEntity(auditId);
        final String nickname = audit.getPlayerName();
        audit.updateStatus(PlayerNameAuditStatus.ALLOWED);
        feedbackRepository.save(new PlayerNameFeedbackEntity(
                nickname,
                true,
                audit.getConfidence(),
                PlayerNameFeedbackEntity.OperatorDecision.ALLOWED,
                null
        ));
        customProfanityRepository.deleteByWord(nickname);
        badWordFiltering.remove(nickname);
        log.info("닉네임 허용 처리: auditId={}, nickname={}", auditId, nickname);
    }

    @Transactional
    public void block(Long auditId) {
        final PlayerNameAuditEntity audit = getAuditEntity(auditId);
        final String nickname = audit.getPlayerName();
        audit.updateStatus(PlayerNameAuditStatus.BLOCKED);

        feedbackRepository.save(new PlayerNameFeedbackEntity(
                nickname,
                true,
                audit.getConfidence(),
                PlayerNameFeedbackEntity.OperatorDecision.BLOCKED,
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

    private PlayerNameAuditEntity getAuditEntity(Long auditId) {
        return auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("검열 항목을 찾을 수 없습니다: " + auditId));
    }
}
