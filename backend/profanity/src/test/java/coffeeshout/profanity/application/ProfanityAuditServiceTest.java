package coffeeshout.profanity.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class ProfanityAuditServiceTest {

    private NicknameAuditRepository auditRepository;
    private ProfanityAuditBatchProcessor batchProcessor;
    private ProfanityWordManagementService profanityWordManagementService;
    private ProfanityAuditService service;

    @BeforeEach
    void setUp() {
        auditRepository = mock(NicknameAuditRepository.class);
        batchProcessor = mock(ProfanityAuditBatchProcessor.class);
        profanityWordManagementService = mock(ProfanityWordManagementService.class);

        final NicknameAuditProperties properties = new NicknameAuditProperties(
                "api-key", "gemini-2.0-flash", 0.8, 10, 5, 2
        );
        service = new ProfanityAuditService(auditRepository, batchProcessor, profanityWordManagementService, properties, new SimpleMeterRegistry());
        service.initMetrics();
    }

    @Nested
    class register_닉네임_등록 {

        @Test
        void 새로운_닉네임은_UNAUDITED_상태로_저장된다() {
            given(auditRepository.existsByNickname("새닉네임"))
                    .willReturn(false);

            service.register("새닉네임");

            then(auditRepository).should().save(any(NicknameAudit.class));
        }

        @Test
        void 이미_등록된_닉네임은_재등록해도_중복_저장되지_않는다() {
            // issue #1467 재현: 이미 검열된(CLEAN 등) 닉네임이 재등장하면, 상태 무관 검사가 없으면
            // 새 UNAUDITED 중복이 생기고 다음 검열 시 (player_name, status) 유니크 충돌이 발생한다.
            // 상태와 무관하게 이미 존재하면 저장하지 않아야 한다.
            given(auditRepository.existsByNickname("이미검열된닉네임"))
                    .willReturn(true);

            service.register("이미검열된닉네임");

            then(auditRepository).should(never()).save(any());
        }

        @Test
        void 운영자_허용_닉네임은_검열_등록이_생략된다() {
            given(profanityWordManagementService.isOperatorAllowed("허용닉네임"))
                    .willReturn(true);

            service.register("허용닉네임");

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
            final NicknameAudit entity = new NicknameAudit("욕설닉네임");
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
