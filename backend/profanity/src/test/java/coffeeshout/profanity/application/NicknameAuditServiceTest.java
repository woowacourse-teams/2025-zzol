package coffeeshout.profanity.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.infra.persistence.audit.NicknameAuditEntity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class NicknameAuditServiceTest {

    private NicknameAuditRepository auditRepository;
    private NicknameAuditBatchProcessor batchProcessor;
    private NicknameAuditService service;

    @BeforeEach
    void setUp() {
        auditRepository = mock(NicknameAuditRepository.class);
        batchProcessor = mock(NicknameAuditBatchProcessor.class);

        final NicknameAuditProperties properties = new NicknameAuditProperties(
                "api-key", "gemini-2.0-flash", 0.8, 10, 5
        );
        service = new NicknameAuditService(auditRepository, batchProcessor, properties, new SimpleMeterRegistry());
        service.initMetrics();
    }

    @Nested
    class register_닉네임_등록 {

        @Test
        void 새로운_닉네임은_UNAUDITED_상태로_저장된다() {
            given(auditRepository.existsByNicknameAndStatus("새닉네임", NicknameAuditStatus.UNAUDITED))
                    .willReturn(false);

            service.register("새닉네임");

            then(auditRepository).should().save(any(NicknameAuditEntity.class));
        }

        @Test
        void 이미_UNAUDITED_상태인_닉네임은_중복_저장되지_않는다() {
            given(auditRepository.existsByNicknameAndStatus("이미등록된닉네임", NicknameAuditStatus.UNAUDITED))
                    .willReturn(true);

            service.register("이미등록된닉네임");

            then(auditRepository).should(never()).save(any());
        }
    }

    @Nested
    class auditPending_배치_검열 {

        @Test
        void UNAUDITED_닉네임이_없으면_배치_처리를_하지_않는다() {
            given(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED)).willReturn(0L);
            given(auditRepository.findByStatusAndAuditedAtIsNull(
                    any(NicknameAuditStatus.class), any(Pageable.class))).willReturn(List.of());

            service.auditPending();

            then(batchProcessor).should(never()).process(any());
        }

        @Test
        void UNAUDITED_닉네임이_있으면_배치_처리가_수행된다() {
            final NicknameAuditEntity entity = new NicknameAuditEntity("욕설닉네임");
            given(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED)).willReturn(1L);
            given(auditRepository.findByStatusAndAuditedAtIsNull(
                    any(NicknameAuditStatus.class), any(Pageable.class)))
                    .willReturn(List.of(entity))
                    .willReturn(List.of());
            given(batchProcessor.process(any())).willReturn(1);

            service.auditPending();

            then(batchProcessor).should().process(List.of(entity));
        }
    }
}
