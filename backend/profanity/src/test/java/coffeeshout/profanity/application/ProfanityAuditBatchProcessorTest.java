package coffeeshout.profanity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import coffeeshout.global.exception.custom.InfrastructureException;
import coffeeshout.global.nickname.ProfanityWordBlockedEvent;
import coffeeshout.profanity.application.port.NicknameAuditRepository;
import coffeeshout.profanity.config.NicknameAuditProperties;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.WordSource;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAudit;
import coffeeshout.profanity.domain.audit.NicknameAuditErrorCode;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ProfanityAuditBatchProcessorTest {

    /** 검열 호출 실패를 견디는 횟수. {@code service.yml}의 nickname-audit.max-attempts. */
    private static final int MAX_ATTEMPTS = 3;

    private NicknameAuditRepository auditRepository;
    private NicknameAuditor nicknameAuditor;
    private ProfanityWordManagementService profanityWordManagementService;
    private ApplicationEventPublisher eventPublisher;
    private TransactionTemplate transactionTemplate;
    private SimpleMeterRegistry meterRegistry;
    private ProfanityAuditBatchProcessor processor;

    @BeforeEach
    void setUp() {
        auditRepository = mock(NicknameAuditRepository.class);
        nicknameAuditor = mock(NicknameAuditor.class);
        profanityWordManagementService = mock(ProfanityWordManagementService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        transactionTemplate = new TransactionTemplate(new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object tx, TransactionDefinition def) {}

            @Override
            protected void doCommit(DefaultTransactionStatus status) {}

            @Override
            protected void doRollback(DefaultTransactionStatus status) {}
        });

        final NicknameAuditProperties properties = new NicknameAuditProperties(
                "test-key",
                "gemini-test",
                0.85,
                10,
                20,
                2,
                Duration.ofSeconds(120),
                Duration.ofMinutes(10),
                MAX_ATTEMPTS);
        meterRegistry = new SimpleMeterRegistry();
        processor = new ProfanityAuditBatchProcessor(
                auditRepository,
                nicknameAuditor,
                profanityWordManagementService,
                eventPublisher,
                meterRegistry,
                transactionTemplate,
                new TextNormalizer(),
                properties);
        processor.initMetrics();
    }

    @Nested
    class FLAGGED_결과_처리 {

        @Test
        void FLAGGED_닉네임은_비속어로_등록되고_차단_이벤트가_발행된다() {
            final NicknameAudit entity = new NicknameAudit("욕설닉네임");
            given(nicknameAuditor.audit(List.of("욕설닉네임")))
                    .willReturn(List.of(new NicknameAuditResult(
                            "욕설닉네임", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "직접 욕설")));
            given(profanityWordManagementService.add("욕설닉네임", Language.KOREAN, WordSource.AI_FLAGGED))
                    .willReturn(true);

            processor.process(List.of(entity));

            then(profanityWordManagementService).should().add("욕설닉네임", Language.KOREAN, WordSource.AI_FLAGGED);
            then(eventPublisher).should().publishEvent(any(ProfanityWordBlockedEvent.class));
        }

        @Test
        void 영어_닉네임은_ENGLISH_언어로_등록된다() {
            final NicknameAudit entity = new NicknameAudit("badword");
            given(nicknameAuditor.audit(List.of("badword")))
                    .willReturn(List.of(new NicknameAuditResult(
                            "badword", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "영어 비속어")));
            given(profanityWordManagementService.add("badword", Language.ENGLISH, WordSource.AI_FLAGGED))
                    .willReturn(true);

            processor.process(List.of(entity));

            then(profanityWordManagementService).should().add("badword", Language.ENGLISH, WordSource.AI_FLAGGED);
        }

        @Test
        void 이미_등록된_단어는_차단_이벤트를_발행하지_않는다() {
            final NicknameAudit entity = new NicknameAudit("욕설닉네임");
            given(nicknameAuditor.audit(List.of("욕설닉네임")))
                    .willReturn(List.of(new NicknameAuditResult(
                            "욕설닉네임", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "직접 욕설")));
            given(profanityWordManagementService.add("욕설닉네임", Language.KOREAN, WordSource.AI_FLAGGED))
                    .willReturn(false);

            processor.process(List.of(entity));

            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        void FLAGGED_처리_후_엔티티_상태가_업데이트된다() {
            final NicknameAudit entity = new NicknameAudit("욕설닉네임");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "욕설닉네임", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "직접 욕설")));

            processor.process(List.of(entity));

            assertThat(entity.getStatus()).isEqualTo(NicknameAuditStatus.FLAGGED);
        }
    }

    @Nested
    class 비속어_조각_추출 {

        @Test
        void 추출된_조각만_등록하고_닉네임_전체는_등록하지_않는다() {
            final NicknameAudit entity = new NicknameAudit("경찬이병신");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "경찬이병신", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "비속어 포함", List.of("병신"))));

            processor.process(List.of(entity));

            then(profanityWordManagementService).should().add("병신", Language.KOREAN, WordSource.AI_FLAGGED);
            then(profanityWordManagementService).should(never()).add(eq("경찬이병신"), any(), any());
        }

        @Test
        void 여러_조각이면_모두_등록된다() {
            final NicknameAudit entity = new NicknameAudit("시발경찬이병신");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "시발경찬이병신",
                            NicknameAuditStatus.FLAGGED,
                            AiConfidence.of(0.95),
                            "비속어 포함",
                            List.of("시발", "병신"))));

            processor.process(List.of(entity));

            then(profanityWordManagementService).should().add("시발", Language.KOREAN, WordSource.AI_FLAGGED);
            then(profanityWordManagementService).should().add("병신", Language.KOREAN, WordSource.AI_FLAGGED);
        }

        @Test
        void 닉네임에_없는_조각은_등록하지_않는다() {
            final NicknameAudit entity = new NicknameAudit("경찬이병신");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "경찬이병신",
                            NicknameAuditStatus.FLAGGED,
                            AiConfidence.of(0.95),
                            "비속어 포함",
                            List.of("병신", "핵상욕설"))));

            processor.process(List.of(entity));

            then(profanityWordManagementService).should().add("병신", Language.KOREAN, WordSource.AI_FLAGGED);
            then(profanityWordManagementService).should(never()).add(eq("핵상욕설"), any(), any());
        }

        @Test
        void 유효한_조각이_없으면_닉네임_전체를_폴백_등록한다() {
            final NicknameAudit entity = new NicknameAudit("씨발놈");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "씨발놈", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "비속어 포함", List.of("닉네임에없는말"))));

            processor.process(List.of(entity));

            then(profanityWordManagementService).should().add("씨발놈", Language.KOREAN, WordSource.AI_FLAGGED);
        }

        @Test
        void 정규화_후_한_글자_조각은_과차단_방지를_위해_제외된다() {
            final NicknameAudit entity = new NicknameAudit("경찬이병신");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "경찬이병신",
                            NicknameAuditStatus.FLAGGED,
                            AiConfidence.of(0.95),
                            "비속어 포함",
                            List.of("병신", "이"))));

            processor.process(List.of(entity));

            then(profanityWordManagementService).should().add("병신", Language.KOREAN, WordSource.AI_FLAGGED);
            then(profanityWordManagementService).should(never()).add(eq("이"), any(), any());
        }

        @Test
        void 정규화_결과가_같은_조각은_한_번만_등록된다() {
            final NicknameAudit entity = new NicknameAudit("시1발놈");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "시1발놈",
                            NicknameAuditStatus.FLAGGED,
                            AiConfidence.of(0.95),
                            "비속어 포함",
                            List.of("시1발", "시i발"))));

            processor.process(List.of(entity));

            // leet 치환(1→i)으로 두 조각의 정규화 결과가 동일 → add는 한 번만 호출
            then(profanityWordManagementService)
                    .should()
                    .add(eq("시1발"), eq(Language.KOREAN), eq(WordSource.AI_FLAGGED));
            then(profanityWordManagementService).should(never()).add(eq("시i발"), any(), any());
        }

        @Test
        void 조각이_비어있으면_닉네임_전체를_폴백_등록한다() {
            final NicknameAudit entity = new NicknameAudit("욕설닉네임");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "욕설닉네임", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "비속어 포함", List.of())));

            processor.process(List.of(entity));

            then(profanityWordManagementService).should().add("욕설닉네임", Language.KOREAN, WordSource.AI_FLAGGED);
        }
    }

    @Nested
    class CLEAN_결과_처리 {

        @Test
        void CLEAN_닉네임은_비속어_등록_없이_상태만_업데이트된다() {
            final NicknameAudit entity = new NicknameAudit("용감한호랑이");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "용감한호랑이", NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반 닉네임")));

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
            final NicknameAudit entity = new NicknameAudit("애매한닉네임");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "애매한닉네임", NicknameAuditStatus.PENDING, AiConfidence.of(0.6), "판단 불명확")));

            processor.process(List.of(entity));

            then(profanityWordManagementService).should(never()).add(any(), any(), any());
            assertThat(entity.getStatus()).isEqualTo(NicknameAuditStatus.PENDING);
        }
    }

    @Nested
    class 빈_결과_처리 {

        @Test
        void AI_응답이_비어있으면_처리_건수_0을_반환한다() {
            final NicknameAudit entity = new NicknameAudit("닉네임");
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
            final List<NicknameAudit> batch = List.of(new NicknameAudit("닉네임1"), new NicknameAudit("닉네임2"));
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(
                            new NicknameAuditResult("닉네임1", NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반"),
                            new NicknameAuditResult("닉네임2", NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반")));

            final int processed = processor.process(batch);

            assertThat(processed).isEqualTo(2);
        }
    }

    @Nested
    class 잔존_UNAUDITED_중복_제거 {

        @Test
        void 이미_terminal_행이_있는_닉네임의_UNAUDITED는_승격_대신_제거된다() {
            // #1467 fix 이전 잔존 데이터: (host, CLEAN)이 이미 존재하는 상태에서 (host, UNAUDITED) 승격 시도.
            final NicknameAudit residual = new NicknameAudit("host");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(
                            new NicknameAuditResult("host", NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반")));
            given(auditRepository.findNicknamesWithTerminalStatus(anyList())).willReturn(Set.of("host"));

            processor.process(List.of(residual));

            then(auditRepository).should().deleteAll(List.of(residual));
            then(auditRepository).should().saveAll(List.of());
            assertThat(residual.getStatus()).isEqualTo(NicknameAuditStatus.UNAUDITED);
        }

        @Test
        void 기존_terminal과_다른_상태로_재판정돼도_모순_행_없이_제거되고_autoBlock도_발동하지_않는다() {
            // 기존 (host, CLEAN)이 있는데 이번 AI 결과가 FLAGGED인 경우. 상태가 달라 유니크 충돌은 없지만,
            // 승격하면 (host, CLEAN)+(host, FLAGGED) 모순 공존 + autoBlock 오발동이 생긴다. 제거가 정답이다.
            final NicknameAudit residual = new NicknameAudit("host");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "host", NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "재판정")));
            given(auditRepository.findNicknamesWithTerminalStatus(anyList())).willReturn(Set.of("host"));

            processor.process(List.of(residual));

            then(auditRepository).should().deleteAll(List.of(residual));
            then(profanityWordManagementService).should(never()).add(any(), any(), any());
            then(eventPublisher).should(never()).publishEvent(any());
            assertThat(residual.getStatus()).isEqualTo(NicknameAuditStatus.UNAUDITED);
        }

        @Test
        void terminal_트윈이_없는_닉네임은_정상_승격된다() {
            final NicknameAudit fresh = new NicknameAudit("용감한호랑이");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(
                            new NicknameAuditResult("용감한호랑이", NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반")));
            given(auditRepository.findNicknamesWithTerminalStatus(anyList())).willReturn(Set.of());

            processor.process(List.of(fresh));

            then(auditRepository).should().saveAll(List.of(fresh));
            then(auditRepository).should(never()).deleteAll(any());
            assertThat(fresh.getStatus()).isEqualTo(NicknameAuditStatus.CLEAN);
        }
    }

    /**
     * 처리 건수는 <b>실제로 UNAUDITED에서 빠져나간 행 수</b>여야 한다.
     *
     * <p>배치 크기를 그대로 돌려주면, 판정을 못 짝지어 그대로 남은 행까지 처리했다고 세게 된다.
     * 그러면 드레인 루프({@code ProfanityAuditService.auditPending})가 진행이 없는데도 같은 0페이지를
     * 계속 다시 읽는다. 배치 전체가 짝을 못 찾으면 13초마다 Gemini를 부르며 영원히 돈다.
     */
    @Nested
    class 처리_건수 {

        @Test
        void 판정을_짝짓지_못한_행은_처리_건수에서_빠진다() {
            final NicknameAudit matched = new NicknameAudit("짝맞는닉");
            final NicknameAudit unmatched = new NicknameAudit("짝없는닉");
            // AI가 요청과 다른 이름을 돌려준 상황. "짝없는닉"에 대응하는 판정이 없다.
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "짝맞는닉", NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반 닉네임")));
            given(auditRepository.findNicknamesWithTerminalStatus(any())).willReturn(Set.of());

            final int processed = processor.process(List.of(matched, unmatched));

            assertThat(processed)
                    .as("판정이 없어 UNAUDITED로 남은 행을 처리했다고 세면 드레인 루프가 멈추지 못한다.")
                    .isEqualTo(1);
        }

        @Test
        void 배치_전체가_짝을_못_찾으면_처리_건수가_0이다() {
            final NicknameAudit first = new NicknameAudit("닉하나");
            final NicknameAudit second = new NicknameAudit("닉둘");
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(List.of(new NicknameAuditResult(
                            "엉뚱한닉", NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반 닉네임")));
            given(auditRepository.findNicknamesWithTerminalStatus(any())).willReturn(Set.of());

            final int processed = processor.process(List.of(first, second));

            assertThat(processed).as("0을 돌려줘야 드레인 루프가 같은 페이지 반복을 멈춘다.").isZero();
        }
    }

    /**
     * 배치 하나의 검열 호출 실패가 회차를 끝내지 않아야 한다.
     *
     * <p>Gemini 응답 파싱 실패는 {@link InfrastructureException}인데 resilience4j ignore 목록이라
     * ({@code resilience4j.yml}의 geminiAudit.ignore-exceptions) 재시도 없이 그대로 올라온다.
     * 여기서 잡지 않으면 남은 적체가 통째로 다음 회차까지 밀린다.
     */
    @Nested
    class 검열_호출_실패 {

        @Test
        void 검열_호출이_예외로_실패해도_예외를_전파하지_않고_0건으로_보고한다() {
            final NicknameAudit entity = new NicknameAudit("닉네임");
            given(nicknameAuditor.audit(anyList()))
                    .willThrow(new InfrastructureException(
                            NicknameAuditErrorCode.AI_RESPONSE_PARSE_FAILED, "닉네임 검열 AI 응답 파싱 실패"));

            final int processed = processor.process(List.of(entity));

            assertThat(processed).isZero();
            then(auditRepository).should(never()).saveAll(any());
        }

        @Test
        void 검열_호출이_실패하면_그_배치_행의_시도_횟수를_올린다() {
            final List<NicknameAudit> batch = List.of(new NicknameAudit("닉하나"), new NicknameAudit("닉둘"));
            given(nicknameAuditor.audit(anyList()))
                    .willThrow(new InfrastructureException(
                            NicknameAuditErrorCode.AI_RESPONSE_PARSE_FAILED, "닉네임 검열 AI 응답 파싱 실패"));

            processor.process(batch);

            final ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.captor();
            then(auditRepository).should().incrementAttemptCount(ids.capture());
            then(auditRepository).should().markDeadLetterAtAttemptLimit(any(), eq(MAX_ATTEMPTS));
            assertThat(ids.getValue()).as("실패한 배치 행만 세야 다른 행이 애먼 상한에 닿지 않는다.").hasSize(batch.size());
        }

        @Test
        void 일시적인_호출_실패는_시도_횟수를_올리지_않는다() {
            // GeminiNicknameAuditor는 네트워크 끊김·레이트리밋·타임아웃을 전부 AI_CALL_FAILED로 감싼다.
            // 이걸 세면 Gemini가 잠깐 죽은 회차 몇 번에 멀쩡한 닉네임이 DEAD_LETTER로 내려간다.
            final NicknameAudit entity = new NicknameAudit("멀쩡닉네임");
            given(nicknameAuditor.audit(anyList()))
                    .willThrow(new InfrastructureException(NicknameAuditErrorCode.AI_CALL_FAILED, "닉네임 검열 AI 호출 실패"));

            processor.process(List.of(entity));

            then(auditRepository).should(never()).incrementAttemptCount(any());
        }

        @Test
        void 상한에_닿아_DEAD_LETTER로_내려간_행은_처리_건수로_보고한다() {
            // DEAD_LETTER는 UNAUDITED 스캔에서 빠진다. 0을 돌려주면 드레인 루프의 페이지 커서가
            // 스캔이 앞으로 당겨진 만큼을 그냥 건너뛴다.
            final NicknameAudit entity = new NicknameAudit("독닉네임");
            given(nicknameAuditor.audit(anyList()))
                    .willThrow(new InfrastructureException(
                            NicknameAuditErrorCode.AI_RESPONSE_PARSE_FAILED, "닉네임 검열 AI 응답 파싱 실패"));
            given(auditRepository.markDeadLetterAtAttemptLimit(any(), eq(MAX_ATTEMPTS)))
                    .willReturn(1);

            final int processed = processor.process(List.of(entity));

            assertThat(processed).isEqualTo(1);
        }

        @Test
        void 시도_횟수_기록이_실패해도_예외를_전파하지_않는다() {
            // 이 기록은 "배치 하나가 회차를 끝내지 않게" 하려고 잡은 자리에서 부른다.
            // 여기서 예외가 다시 올라가면 그 try/catch가 무의미해진다.
            final NicknameAudit entity = new NicknameAudit("닉네임");
            given(nicknameAuditor.audit(anyList()))
                    .willThrow(new InfrastructureException(
                            NicknameAuditErrorCode.AI_RESPONSE_PARSE_FAILED, "닉네임 검열 AI 응답 파싱 실패"));
            willThrow(new DataIntegrityViolationException("DB 오류"))
                    .given(auditRepository)
                    .incrementAttemptCount(any());

            final int processed = processor.process(List.of(entity));

            assertThat(processed).isZero();
        }

        @Test
        void DEAD_LETTER로_내려간_행_수를_메트릭으로_남긴다() {
            final NicknameAudit entity = new NicknameAudit("독닉네임");
            given(nicknameAuditor.audit(anyList()))
                    .willThrow(new InfrastructureException(
                            NicknameAuditErrorCode.AI_RESPONSE_PARSE_FAILED, "닉네임 검열 AI 응답 파싱 실패"));
            given(auditRepository.markDeadLetterAtAttemptLimit(any(), eq(MAX_ATTEMPTS)))
                    .willReturn(2);

            processor.process(List.of(entity));

            assertThat(meterRegistry.counter("nickname.audit.dead.lettered").count())
                    .isEqualTo(2.0);
        }
    }

    /**
     * 벌크 저장이 한 행에서 터지면 배치 전체가 롤백된다. 이미 받아둔 Gemini 판정까지 버리면
     * 다음 회차에 같은 닉네임을 다시 사야 한다. 판정을 들고 한 건씩 다시 저장해야 한다.
     */
    @Nested
    class 벌크_저장_실패 {

        @Test
        void 벌크_저장이_실패하면_판정을_버리지_않고_건별로_저장한다() {
            final List<NicknameAudit> batch = List.of(new NicknameAudit("닉하나"), new NicknameAudit("닉둘"));
            givenCleanResultsFor("닉하나", "닉둘");
            givenBulkSaveFails();

            final int processed = processor.process(batch);

            assertThat(processed).isEqualTo(2);
            then(auditRepository).should().save(batch.get(0));
            then(auditRepository).should().save(batch.get(1));
            // 판정을 다시 사면 같은 배치에 Gemini 호출이 두 번 나간다. 폴백은 들고 있는 판정만 쓴다.
            then(nicknameAuditor).should(times(1)).audit(anyList());
        }

        @Test
        void 건별_저장도_실패하는_행만_UNAUDITED로_남는다() {
            final NicknameAudit healthy = new NicknameAudit("닉하나");
            final NicknameAudit trouble = new NicknameAudit("말썽닉");
            givenCleanResultsFor("닉하나", "말썽닉");
            givenBulkSaveFails();
            willThrow(new DataIntegrityViolationException("uq_player_name_audit_name_status 충돌"))
                    .given(auditRepository)
                    .save(trouble);

            final int processed = processor.process(List.of(healthy, trouble));

            assertThat(processed).as("저장에 성공한 행만 UNAUDITED에서 빠진다.").isEqualTo(1);
        }

        /**
         * 벌크 트랜잭션이 커밋에 성공하고도 예외를 낼 수 있다. {@code ProfanityWordBlockedEvent} 수신자가
         * AFTER_COMMIT + REQUIRES_NEW라 거기서 난 예외가 커밋 뒤에 올라온다. 폴백이 판정을 다시 계산하면
         * 방금 커밋한 행을 자기 자신의 terminal 트윈으로 착각해 지워버린다.
         */
        @Test
        void 폴백은_이미_검열된_행을_중복으로_보고_지우지_않는다() {
            final NicknameAudit entity = new NicknameAudit("닉하나");
            givenCleanResultsFor("닉하나");
            givenBulkSaveFails();
            // 커밋이 끝나 자기 행이 이미 CLEAN으로 남아 있는 상태를 흉내낸다.
            given(auditRepository.findNicknamesWithTerminalStatus(any())).willReturn(Set.of("닉하나"));

            processor.process(List.of(entity));

            then(auditRepository).should(never()).deleteAll(any());
            then(auditRepository).should().save(entity);
        }

        @Test
        void 폴백이_돌아도_판정_메트릭은_행마다_한_번만_오른다() {
            final List<NicknameAudit> batch = List.of(new NicknameAudit("닉하나"), new NicknameAudit("닉둘"));
            givenCleanResultsFor("닉하나", "닉둘");
            givenBulkSaveFails();

            processor.process(batch);

            assertThat(meterRegistry
                            .counter("nickname.audit.result", "status", NicknameAuditStatus.CLEAN.name())
                            .count())
                    .as("판정을 반영하는 자리에서 올리면 벌크 시도와 폴백에서 두 번 세어진다.")
                    .isEqualTo(2.0);
        }

        private void givenCleanResultsFor(String... nicknames) {
            given(nicknameAuditor.audit(anyList()))
                    .willReturn(Arrays.stream(nicknames)
                            .map(nickname -> new NicknameAuditResult(
                                    nickname, NicknameAuditStatus.CLEAN, AiConfidence.of(0.99), "일반 닉네임"))
                            .toList());
        }

        private void givenBulkSaveFails() {
            willThrow(new DataIntegrityViolationException("uq_player_name_audit_name_status 충돌"))
                    .given(auditRepository)
                    .saveAll(any());
        }
    }
}
