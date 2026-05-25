package coffeeshout.room.application.service.player.name;

import coffeeshout.profanity.domain.audit.NicknameSubmittedEvent;
import coffeeshout.user.domain.event.UserNicknameRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserNicknameAuditListener {

    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserNicknameRegistered(UserNicknameRegisteredEvent event) {
        log.debug("닉네임 검열 이벤트 발행: {}", event.nickname());
        eventPublisher.publishEvent(new NicknameSubmittedEvent(event.nickname()));
    }
}
