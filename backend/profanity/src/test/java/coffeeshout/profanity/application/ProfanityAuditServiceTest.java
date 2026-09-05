package coffeeshout.profanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
                "api-key", "gemini-2.0-flash", 0.8, 10, 5, 2, Duration.ofSeconds(120), Duration.ofMinutes(10));
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
     * 드레인 루프에 회차 시간 예산이 있는지 검증한다.
     *
     * <p>배치 하나가 Gemini 호출 하나이고 레이트리미터가 13초에 하나만 허용하므로
     * ({@code resilience4j.yml}의 geminiAudit.limit-refresh-period) 회차 소요가 적체량에 정비례한다.
     * 예산이 없으면 적체 10만 건에 3.6시간을 도는데 그동안 실행기 스레드를 붙잡고 있게 된다.
     */
    @Nested
    class auditPending_한_회차_소요시간 {

        /** 레이트리미터가 허용하는 최소 간격. {@code resilience4j.yml}의 geminiAudit.limit-refresh-period = 13s. */
        private static final int SECONDS_PER_BATCH = 13;

        /** 프로덕션 배치 크기. {@code service.yml}의 nickname-audit.batch-size. */
        private static final int PRODUCTION_BATCH_SIZE = 100;

        /** 회차 예산. {@code service.yml}의 nickname-audit.max-run-duration. */
        private static final int MAX_RUN_SECONDS = 600;

        private static final int BACKLOG = 10_000;

        @Test
        void 적체가_커도_한_회차는_시간_예산_안에서_끝난다() {
            final StubClock clock = new StubClock(Instant.parse("2026-09-03T00:00:00Z"));
            final Instant startedAt = clock.instant();
            final ProfanityAuditService target = productionSizedService(clock);
            final AtomicInteger remaining = new AtomicInteger(BACKLOG);

            given(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED))
                    .willReturn((long) BACKLOG);
            given(auditRepository.findByStatusAndAuditedAtIsNull(any(NicknameAuditStatus.class), any(Pageable.class)))
                    .willAnswer(invocation -> nextBatch(remaining));
            given(batchProcessor.process(any())).willAnswer(invocation -> {
                final List<NicknameAudit> batch = invocation.getArgument(0);
                clock.advance(Duration.ofSeconds(SECONDS_PER_BATCH));
                return batch.size();
            });

            target.auditPending();

            assertThat(Duration.between(startedAt, clock.instant()).toSeconds())
                    .as("적체 %,d건 회차가 붙잡은 시간(초). 예산 확인이 배치 사이에 걸리므로 한 배치까지 초과할 수 있다.", BACKLOG)
                    .isLessThanOrEqualTo(MAX_RUN_SECONDS + SECONDS_PER_BATCH);
        }

        @Test
        void 진행이_없으면_같은_페이지를_반복해_읽지_않는다() {
            final StubClock clock = new StubClock(Instant.parse("2026-09-03T00:00:00Z"));
            final ProfanityAuditService target = productionSizedService(clock);

            given(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED))
                    .willReturn((long) PRODUCTION_BATCH_SIZE);
            given(auditRepository.findByStatusAndAuditedAtIsNull(any(NicknameAuditStatus.class), any(Pageable.class)))
                    .willAnswer(invocation -> batchOf(PRODUCTION_BATCH_SIZE));
            // 배치 전체가 판정을 못 받아 UNAUDITED로 남은 상황. 처리 건수가 0이면 다시 읽어봐야 같은 행이다.
            given(batchProcessor.process(any())).willReturn(0);

            target.auditPending();

            then(batchProcessor).should(times(1)).process(any());
        }

        private ProfanityAuditService productionSizedService(Clock clock) {
            final NicknameAuditProperties production = new NicknameAuditProperties(
                    "api-key",
                    "gemini-3.5-flash",
                    0.85,
                    PRODUCTION_BATCH_SIZE,
                    20,
                    2,
                    Duration.ofSeconds(120),
                    Duration.ofSeconds(MAX_RUN_SECONDS));
            final ProfanityAuditService target = new ProfanityAuditService(
                    auditRepository,
                    batchProcessor,
                    profanityWordManagementService,
                    production,
                    new SimpleMeterRegistry(),
                    clock);
            target.initMetrics();
            return target;
        }

        private List<NicknameAudit> nextBatch(AtomicInteger remaining) {
            final int take = Math.min(PRODUCTION_BATCH_SIZE, remaining.get());
            remaining.addAndGet(-take);
            return batchOf(take);
        }

        private List<NicknameAudit> batchOf(int size) {
            final List<NicknameAudit> batch = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                batch.add(new NicknameAudit("닉" + i));
            }
            return batch;
        }
    }

    /**
     * 종료 신호가 회차를 끊는지 검증한다.
     *
     * <p>회차는 전용 실행기에서 돌고 그 실행기는 destroyMethod가 {@code shutdownNow}다. 종료 시 회차 스레드에
     * 인터럽트가 오는데, 루프가 그걸 안 보면 컨텍스트 종료가 회차 예산(기본 10분)만큼 밀린다.
     * Blue/Green 전환에서 구 컨테이너가 그만큼 늦게 내려가거나 SIGKILL을 맞는다.
     */
    @Nested
    class auditPending_종료_요청 {

        @Test
        void 인터럽트가_걸려_있으면_배치를_시작하지_않는다() {
            given(auditRepository.countByStatusAndAuditedAtIsNull(NicknameAuditStatus.UNAUDITED))
                    .willReturn(1L);
            given(auditRepository.findByStatusAndAuditedAtIsNull(any(NicknameAuditStatus.class), any(Pageable.class)))
                    .willReturn(List.of(new NicknameAudit("닉네임")));

            Thread.currentThread().interrupt();
            try {
                service.auditPending();
            } finally {
                // 다음 테스트로 새지 않게 플래그를 지운다.
                Thread.interrupted();
            }

            then(batchProcessor).should(never()).process(any());
        }
    }

    /** 배치 소요 시간을 흉내내기 위한 수동 진행 시계. */
    private static final class StubClock extends Clock {

        private Instant now;

        private StubClock(Instant start) {
            this.now = start;
        }

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
