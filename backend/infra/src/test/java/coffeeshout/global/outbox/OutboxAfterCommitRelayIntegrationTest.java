package coffeeshout.global.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import coffeeshout.InfraModuleIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * OutboxAfterCommitRelay 통합 테스트.
 * <p>
 * 단위 테스트로 검증 불가한 @TransactionalEventListener(phase=AFTER_COMMIT) 바인딩을 검증한다.
 * 트랜잭션이 실제로 커밋된 뒤에만 릴레이가 실행되는지 확인한다.
 * outboxAfterCommitRelay spy는 베이스 클래스(InfraModuleIntegrationTest)가 제공한다.
 */
class OutboxAfterCommitRelayIntegrationTest extends InfraModuleIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUpTx() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void 트랜잭션_커밋_후_AFTER_COMMIT_리스너가_실행된다() {
        // given
        OutboxSavedEvent savedEvent = new OutboxSavedEvent(42L, "test-stream", "{}", null);
        // 실제 처리 로직은 단위 테스트에서 검증하므로 여기서는 바인딩 타이밍만 확인
        doNothing().when(outboxAfterCommitRelay).onOutboxSaved(any(OutboxSavedEvent.class));

        // when — 트랜잭션 내에서 이벤트 발행 후 커밋
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(savedEvent));

        // then — AFTER_COMMIT 리스너가 호출되어야 한다
        verify(outboxAfterCommitRelay).onOutboxSaved(eq(savedEvent));
    }
}
