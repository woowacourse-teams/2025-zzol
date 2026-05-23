package coffeeshout.room.application.service.player.name;

import coffeeshout.profanity.application.ProfanityWordManagementService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.global.event.ProfanityWordBlockedEvent;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.room.domain.RoomErrorCode;
import coffeeshout.room.domain.audit.PlayerNameAuditStatus;
import coffeeshout.room.application.port.PlayerNameAuditRepository;
import coffeeshout.room.application.port.PlayerNameFeedbackRepository;
import coffeeshout.room.infra.persistence.nickname.PlayerNameAuditEntity;
import coffeeshout.room.infra.persistence.nickname.PlayerNameFeedbackEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerNameFeedbackService {

    private final PlayerNameAuditRepository auditRepository;
    private final PlayerNameFeedbackRepository feedbackRepository;
    private final ProfanityWordManagementService profanityWordManagementService;
    private final ApplicationEventPublisher eventPublisher;

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
        profanityWordManagementService.deactivate(nickname);
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
        profanityWordManagementService.add(nickname, Language.KOREAN, WordSource.MANUAL);
        eventPublisher.publishEvent(new ProfanityWordBlockedEvent(nickname));
        log.info("닉네임 차단 처리: auditId={}, nickname={}", auditId, nickname);
    }

    private PlayerNameAuditEntity getAuditEntity(Long auditId) {
        return auditRepository.findById(auditId)
                .orElseThrow(() -> new BusinessException(
                        RoomErrorCode.NO_EXIST_PLAYER_NAME_AUDIT, "검열 항목을 찾을 수 없습니다: " + auditId));
    }
}
