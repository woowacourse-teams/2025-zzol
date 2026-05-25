package coffeeshout.profanity.application;

import coffeeshout.profanity.domain.audit.NicknameSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NicknameSubmittedEventListener {

    private final NicknameAuditService nicknameAuditService;

    @EventListener
    @Transactional
    public void onNicknameSubmitted(NicknameSubmittedEvent event) {
        log.debug("닉네임 검열 등록 요청 수신: {}", event.nickname());
        nicknameAuditService.register(event.nickname());
    }
}
