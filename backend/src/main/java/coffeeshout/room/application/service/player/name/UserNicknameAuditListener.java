package coffeeshout.room.application.service.player.name;

import coffeeshout.user.domain.event.UserNicknameRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserNicknameAuditListener {

    private final PlayerNameAuditService playerNameAuditService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserNicknameRegistered(UserNicknameRegisteredEvent event) {
        log.debug("사용자 닉네임 AI 검열 등록 요청 수신");
        playerNameAuditService.register(event.nickname());
    }
}
