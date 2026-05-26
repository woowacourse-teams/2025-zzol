package coffeeshout.profanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.global.event.ProfanityWordBlockedEvent;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.domain.Language;

import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditEntity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ProfanityAuditBatchProcessorTest {

    private NicknameAuditRepository auditRepository;
    private NicknameAuditor nicknameAuditor;
    private ProfanityWordManagementService profanityWordManagementService;
    private ApplicationEventPublisher eventPublisher;
    private TransactionTemplate transactionTemplate;
    private ProfanityAuditBatchProcessor processor;

    @BeforeEach
    void setUp() {
        auditRepository = mock(NicknameAuditRepository.class);
        nicknameAuditor = mock(NicknameAuditor.class);
        profanityWordManagementService = mock(ProfanityWordManagementService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        transactionTemplate = new TransactionTemplate(new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object tx, TransactionDefinition def) {}
            @Override protected void doCommit(DefaultTransactionStatus status) {}
            @Override protected void doRollback(DefaultTransactionStatus status) {}
        });

        processor = new ProfanityAuditBatchProcessor(
                auditRepository, nicknameAuditor, profanityWordManagementService,
                eventPublisher, new SimpleMeterRegistry(), transactionTemplate
        );
        processor.initMetrics();
    }

    @Nested
    class FLAGGED_결과_처리 {

        @Test
        void FLAGGED_닉네임은_비속어로_등록되고_차단_이벤트가_발행된다() {
            final NicknameAuditEntity entity = new NicknameAuditEntity("욕설닉네임");
            given(nicknameAuditor.audit(List.of("욕설닉네임"))).willReturn(List.of(
                    new NicknameAuditResult("욕설닉네임", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "직접 욕설")
            ));
            given(profanityWordManagementService.add("욕설닉네임", Language.KOREAN, WordSource.AI_FLAGGED)).willReturn(true);

            processor.process(List.of(entity));

            then(profanityWordManagementService).should().add("욕설닉네임", Language.KOREAN, WordSource.AI_FLAGGED);
            then(eventPublisher).should().publishEvent(any(ProfanityWordBlockedEvent.class));
        }

        @Test
        void 이미_등록된_단어는_차단_이벤트를_발행하지_않는다() {
            final NicknameAuditEntity entity = new NicknameAuditEntity("욕설닉네임");
            given(nicknameAuditor.audit(List.of("욕설닉네임"))).willReturn(List.of(
                    new NicknameAuditResult("욕설닉네임", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "직접 욕설")
            ));
            given(profanityWordManagementService.add("욕설닉네임", Language.KOREAN, WordSource.AI_FLAGGED)).willReturn(false);

            processor.process(List.of(entity));

            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        void FLAGGED_처리_후_엔티티_상태가_업데이트된다() {
            final NicknameAuditEntity entity = new NicknameAuditEntity("욕설닉네임");
            given(nicknameAuditor.audit(anyList())).willReturn(List.of(
                    new NicknameAuditResult("욕설닉네임", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "직접 욕설")
            ));

            processor.process(List.of(entity));

            assertThat(entity.getStatus()).isEqualTo(NicknameAuditStatus.FLAGGED);
        }
    }

    @Nested
    class CLEAN_결과_처리 {

        @Test
        void CLEAN_닉네임은_비속어_등록_없이_상태만_업데이트된다() {
            final NicknameAuditEntity entity = new NicknameAuditEntity("용감한호랑이");
            given(nicknameAuditor.audit(anyList())).willReturn(List.of(
                    new NicknameAuditResult("용감한호랑이", NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반 닉네임")
            ));

            processor.process(List.of(entity));

            then(profanityWordManagementService).should(never()).add(any(), any(), any());
            then(eventPublisher).should(never()).publishEvent(any());
            assertThat(entity.getStatus()).isEqualTo(NicknameAuditStatus.CLEAN);
        }
    }

    @Nested
    class PENDING_결과_처리 {

        @Test
        void PENDING_닉네임은_비속어_등록_없이_상태만_업데이트된다() {
            final NicknameAuditEntity entity = new NicknameAuditEntity("애매한닉네임");
            given(nicknameAuditor.audit(anyList())).willReturn(List.of(
                    new NicknameAuditResult("애매한닉네임", NicknameAuditStatus.PENDING, AiConfidence.of(0.6), "판단 불명확")
            ));

            processor.process(List.of(entity));

            then(profanityWordManagementService).should(never()).add(any(), any(), any());
            assertThat(entity.getStatus()).isEqualTo(NicknameAuditStatus.PENDING);
        }
    }

    @Nested
    class 빈_결과_처리 {

        @Test
        void AI_응답이_비어있으면_처리_건수_0을_반환한다() {
            final NicknameAuditEntity entity = new NicknameAuditEntity("닉네임");
            given(nicknameAuditor.audit(anyList())).willReturn(List.of());

            final int processed = processor.process(List.of(entity));

            assertThat(processed).isZero();
            then(auditRepository).should(never()).saveAll(any());
        }
    }

    @Nested
    class 배치_처리_반환값 {

        @Test
        void 처리된_엔티티_수를_반환한다() {
            final List<NicknameAuditEntity> batch = List.of(
                    new NicknameAuditEntity("닉네임1"),
                    new NicknameAuditEntity("닉네임2")
            );
            given(nicknameAuditor.audit(anyList())).willReturn(List.of(
                    new NicknameAuditResult("닉네임1", NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반"),
                    new NicknameAuditResult("닉네임2", NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반")
            ));

            final int processed = processor.process(batch);

            assertThat(processed).isEqualTo(2);
        }
    }
}
