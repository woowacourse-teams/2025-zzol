package coffeeshout.profanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

        final NicknameAuditProperties properties =
                new NicknameAuditProperties("api-key", "gemini-2.0-flash", 0.8, 10, 5, 2);
        service = new ProfanityAuditService(
                auditRepository,
                batchProcessor,
                profanityWordManagementService,
                properties,
                new SimpleMeterRegistry(),
                Clock.systemDefaultZone());
        service.initMetrics();
    }

    @Nested
    class register_닉네임_등록 {

        @Test
        void 새로운_닉네임은_UNAUDITED_상태로_저장된다() {
            given(auditRepository.existsByNickname("새닉네임")).willReturn(false);

            service.register("새닉네임");

            then(auditRepository).should().insertUnaudited(eq("새닉네임"), any(Instant.class));
        }

        @Test
        void 이미_등록된_닉네임은_재등록해도_중복_저장되지_않는다() {
            // issue #1467 재현: 이미 검열된(CLEAN 등) 닉네임이 재등장하면, 상태 무관 검사가 없으면
            // 새 UNAUDITED 중복이 생기고 다음 검열 시 (player_name, status) 유니크 충돌이 발생한다.
            // 상태와 무관하게 이미 존재하면 저장하지 않아야 한다.
            given(auditRepository.existsByNickname("이미검열된닉네임")).willReturn(true);

            service.register("이미검열된닉네임");

            then(auditRepository).should(never()).insertUnaudited(any(), any());
        }

        @Test
        void 운영자_허용_닉네임은_검열_등록이_생략된다() {
            given(profanityWordManagementService.isOperatorAllowed("허용닉네임")).willReturn(true);

            service.register("허용닉네임");

            then(auditRepository).should(never()).insertUnaudited(any(), any());
        }
    }

    @Nested
    class auditPending_배치_검열 {

        @Test
        void UNAUDITED_닉네임이_없으면_배치_처리를_하지_않는다() {
            given(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED))
                    .willReturn(0L);
            given(auditRepository.findByStatusAndAuditedAtIsNull(any(NicknameAuditStatus.class), any(Pageable.class)))
                    .willReturn(List.of());

            service.auditPending();

            then(batchProcessor).should(never()).process(any());
        }

        @Test
        void UNAUDITED_닉네임이_있으면_배치_처리가_수행된다() {
            final NicknameAudit entity = new NicknameAudit("욕설닉네임");
            given(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED))
                    .willReturn(1L);
            given(auditRepository.findByStatusAndAuditedAtIsNull(any(NicknameAuditStatus.class), any(Pageable.class)))
                    .willReturn(List.of(entity))
                    .willReturn(List.of());
            given(batchProcessor.process(any())).willReturn(1);

            service.auditPending();

            then(batchProcessor).should().process(List.of(entity));
        }
    }

    /**
     * 재현 테스트 — 드레인 루프에 시간 예산이 없다.
     *
     * <p>{@code auditPending()}은 큐가 빌 때까지 배치를 연속으로 돈다. 배치 하나는 Gemini 호출 하나이고,
     * 레이트리미터가 13초에 한 번만 허용하므로({@code resilience4j.yml}의 {@code geminiAudit.limit-refresh-period})
     * 한 회차 소요 시간은 적체량에 정비례한다. 상한이 없어 적체가 크면 회차가 끝없이 길어진다.
     *
     * <p>이게 왜 위험한가. 프로덕션의 {@code @Scheduled}는 전부 스레드 하나를 공유한다
     * ({@code DelayRemovalSchedulerConfig}가 유일한 {@code TaskScheduler} 빈이라 Spring Boot 자동설정이 물러난다).
     * 그 안에 500ms 주기 Outbox 릴레이가 있다. 감사 회차가 길어지는 만큼 릴레이가 멈춘다.
     *
     * <p>수정 방향은 회차당 시간 예산 또는 최대 배치 수를 두는 것이다.
     */
    @Nested
    class auditPending_한_회차_소요시간 {

        /** 레이트리미터가 허용하는 최소 간격. {@code resilience4j.yml}의 geminiAudit.limit-refresh-period = 13s. */
        private static final int SECONDS_PER_BATCH = 13;

        /** 프로덕션 배치 크기. {@code service.yml}의 nickname-audit.batch-size. */
        private static final int PRODUCTION_BATCH_SIZE = 100;

        /** 한 회차가 공유 스케줄러 스레드를 붙잡아도 되는 상한. 이보다 길면 Outbox 릴레이가 굶는다. */
        private static final int MAX_RUN_SECONDS = 600;

        private static final int BACKLOG = 10_000;

        @Test
        void 적체가_크면_한_회차가_시간_예산을_넘긴다() {
            final ProfanityAuditService target = productionSizedService();
            final AtomicInteger remaining = new AtomicInteger(BACKLOG);
            final AtomicLong simulatedSeconds = new AtomicLong();

            given(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED))
                    .willReturn((long) BACKLOG);
            given(auditRepository.findByStatusAndAuditedAtIsNull(any(NicknameAuditStatus.class), any(Pageable.class)))
                    .willAnswer(invocation -> nextBatch(remaining));
            given(batchProcessor.process(any())).willAnswer(invocation -> {
                final List<NicknameAudit> batch = invocation.getArgument(0);
                simulatedSeconds.addAndGet(SECONDS_PER_BATCH);
                return batch.size();
            });

            target.auditPending();

            assertThat(simulatedSeconds.get())
                    .as("적체 %,d건을 비우는 데 걸리는 초. 이 시간 동안 공유 스케줄러 스레드가 묶인다.", BACKLOG)
                    .isLessThanOrEqualTo(MAX_RUN_SECONDS);
        }

        private ProfanityAuditService productionSizedService() {
            final NicknameAuditProperties production =
                    new NicknameAuditProperties("api-key", "gemini-3.5-flash", 0.85, PRODUCTION_BATCH_SIZE, 20, 2);
            final ProfanityAuditService target = new ProfanityAuditService(
                    auditRepository,
                    batchProcessor,
                    profanityWordManagementService,
                    production,
                    new SimpleMeterRegistry(),
                    Clock.systemDefaultZone());
            target.initMetrics();
            return target;
        }

        private List<NicknameAudit> nextBatch(AtomicInteger remaining) {
            final int take = Math.min(PRODUCTION_BATCH_SIZE, remaining.get());
            remaining.addAndGet(-take);
            final List<NicknameAudit> batch = new ArrayList<>(take);
            for (int i = 0; i < take; i++) {
                batch.add(new NicknameAudit("닉" + i));
            }
            return batch;
        }
    }
}
