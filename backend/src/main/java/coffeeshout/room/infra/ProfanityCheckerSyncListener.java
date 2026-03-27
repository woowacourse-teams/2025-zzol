package coffeeshout.room.infra;

import coffeeshout.room.domain.service.ProfanityChecker;
import coffeeshout.room.infra.event.ProfanityWordAllowedEvent;
import coffeeshout.room.infra.event.ProfanityWordBlockedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfanityCheckerSyncListener {

    private final ProfanityChecker profanityChecker;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWordBlocked(ProfanityWordBlockedEvent event) {
        profanityChecker.add(event.word());
        log.info("커스텀 비속어 등록: {}", event.word());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWordAllowed(ProfanityWordAllowedEvent event) {
        profanityChecker.remove(event.word());
        log.info("커스텀 비속어 제거: {}", event.word());
    }
}
