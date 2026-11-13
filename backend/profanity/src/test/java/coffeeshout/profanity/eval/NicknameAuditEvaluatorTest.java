package coffeeshout.profanity.eval;

import static coffeeshout.profanity.fixture.NicknameGoldenSetFixture.경계;
import static coffeeshout.profanity.fixture.NicknameGoldenSetFixture.욕설;
import static coffeeshout.profanity.fixture.NicknameGoldenSetFixture.우회;
import static coffeeshout.profanity.fixture.NicknameGoldenSetFixture.정상;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import coffeeshout.profanity.eval.GoldenItem.Expected;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.CallRecord;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.Mismatch;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.Predicted;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.Rate;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.RunRow;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.Scores;
import coffeeshout.profanity.eval.NicknameAuditEvaluation.TokenUsage;
import coffeeshout.profanity.fixture.NicknameAuditPropertiesFixture;
import coffeeshout.profanity.fixture.NicknameGoldenSetFixture;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NicknameAuditEvaluatorTest {

    // 운영 설정과 같은 조각 최소 길이를 쓴다
    private static final int 조각_최소_길이 =
            NicknameAuditPropertiesFixture.API_키("api-key").minTermLength();

    private final NicknameAuditEvaluator evaluator = new NicknameAuditEvaluator(new TextNormalizer(), 조각_최소_길이);

    private static NicknameAuditResult 판정(String nickname, NicknameAuditStatus status, String... terms) {
        return new NicknameAuditResult(nickname, status, AiConfidence.of(0.9), "사유", List.of(terms));
    }

    /** 닉네임별로 정해 둔 판정을 돌려준다. 판정을 정하지 않은 닉네임은 결과에서 뺀다. */
    private static NicknameAuditor 고정_검열기(Map<String, NicknameAuditResult> results) {
        return nicknames -> nicknames.stream()
                .filter(results::containsKey)
                .map(results::get)
                .toList();
    }

    @Nested
    class 첫_회차_지표 {

        private static final List<GoldenItem> 항목 = List.of(
                욕설("씨발놈", "직접욕설", "씨발"),
                욕설("개새끼야", "직접욕설", "개새끼"),
                욕설("병신같다", "직접욕설", "병신"),
                우회("ㅅㅂ놈아", "초성", "ㅅㅂ"),
                우회("씨.발", "특수문자삽입", "씨.발"),
                경계("미친존재감"),
                경계("개쩌는형"),
                정상("용감한호랑이", "일반닉네임"),
                정상("시발점", "오탐유발정상어"),
                정상("미쳤다", "오탐유발정상어"));

        // 병신같다는 판정을 돌려주지 않아 MISSING이 된다
        private static final NicknameAuditor 검열기 = 고정_검열기(Map.of(
                "씨발놈", 판정("씨발놈", NicknameAuditStatus.FLAGGED, "씨-발"),
                "개새끼야", 판정("개새끼야", NicknameAuditStatus.FLAGGED, "새끼"),
                "ㅅㅂ놈아", 판정("ㅅㅂ놈아", NicknameAuditStatus.PENDING),
                "씨.발", 판정("씨.발", NicknameAuditStatus.CLEAN),
                "미친존재감", 판정("미친존재감", NicknameAuditStatus.PENDING),
                "개쩌는형", 판정("개쩌는형", NicknameAuditStatus.CLEAN),
                "용감한호랑이", 판정("용감한호랑이", NicknameAuditStatus.CLEAN),
                "시발점", 판정("시발점", NicknameAuditStatus.FLAGGED, "시발"),
                "미쳤다", 판정("미쳤다", NicknameAuditStatus.PENDING)));

        private final NicknameAuditEvaluation 결과 = evaluator.evaluate(항목, 검열기, 100, 1);

        @Test
        void 경계와_MISSING을_뺀_정답으로_정밀도와_재현율을_잰다() {
            assertThat(결과.overall())
                    .isEqualTo(new Scores(new Rate(3, 5), new Rate(3, 4), new Rate(2, 3), new Rate(2, 4)));
        }

        @Test
        void 카테고리별로_따로_잰다() {
            assertSoftly(softly -> {
                softly.assertThat(결과.byCategory()).containsOnlyKeys("직접욕설", "초성", "특수문자삽입", "경계", "일반닉네임", "오탐유발정상어");
                softly.assertThat(결과.byCategory().get("직접욕설"))
                        .isEqualTo(new Scores(new Rate(2, 2), new Rate(2, 2), new Rate(2, 2), new Rate(2, 2)));
                softly.assertThat(결과.byCategory().get("오탐유발정상어"))
                        .isEqualTo(new Scores(new Rate(0, 2), new Rate(0, 0), new Rate(0, 1), new Rate(0, 0)));
            });
        }

        @Test
        void 혼동행렬은_결과가_없는_닉네임을_MISSING으로_센다() {
            assertSoftly(softly -> {
                softly.assertThat(결과.confusion().get(Expected.PROFANE))
                        .containsExactlyInAnyOrderEntriesOf(Map.of(
                                Predicted.CLEAN, 0, Predicted.FLAGGED, 2, Predicted.PENDING, 0, Predicted.MISSING, 1));
                softly.assertThat(결과.confusion().get(Expected.EVASION))
                        .containsExactlyInAnyOrderEntriesOf(Map.of(
                                Predicted.CLEAN, 1, Predicted.FLAGGED, 0, Predicted.PENDING, 1, Predicted.MISSING, 0));
                softly.assertThat(결과.confusion().get(Expected.AMBIGUOUS))
                        .containsExactlyInAnyOrderEntriesOf(Map.of(
                                Predicted.CLEAN, 1, Predicted.FLAGGED, 0, Predicted.PENDING, 1, Predicted.MISSING, 0));
                softly.assertThat(결과.confusion().get(Expected.CLEAN))
                        .containsExactlyInAnyOrderEntriesOf(Map.of(
                                Predicted.CLEAN, 1, Predicted.FLAGGED, 1, Predicted.PENDING, 1, Predicted.MISSING, 0));
            });
        }

        @Test
        void 오탐률_미탐률_경계_포착률은_판정을_받은_닉네임으로만_잰다() {
            assertSoftly(softly -> {
                softly.assertThat(결과.cleanFalsePositive()).isEqualTo(new Rate(2, 3));
                // 판정을 못 받은 병신같다는 미탐이 아니라 분모에서 빠진다
                softly.assertThat(결과.positiveMiss()).isEqualTo(new Rate(1, 4));
                softly.assertThat(결과.ambiguousCapture()).isEqualTo(new Rate(1, 2));
            });
        }

        @Test
        void 조각_정확도는_FLAGGED로_잡은_욕설에서_정답_조각과_같은_비율이다() {
            // 씨발놈은 표기가 달라도 정규화하면 같아 맞고, 개새끼야는 새끼만 뽑아 틀렸다
            assertThat(결과.termAccuracy()).isEqualTo(new Rate(1, 2));
        }

        @Test
        void 이상_판정과_다른_닉네임을_모은다() {
            assertThat(결과.mismatches())
                    .extracting(Mismatch::nickname)
                    .containsExactly("병신같다", "ㅅㅂ놈아", "씨.발", "개쩌는형", "시발점", "미쳤다");
        }

        @Test
        void 한_번만_돌리면_흔들림은_잴_수_없다() {
            assertThat(결과.instability().toString()).isEqualTo("n/a");
        }

        @Test
        void 마크다운_리포트에_지표와_오답이_들어간다() {
            final String 리포트 = 결과.toMarkdown("가짜 검열기");

            assertSoftly(softly -> {
                softly.assertThat(리포트).contains("| 걸러냄 정밀도 (FLAGGED+PENDING) | 60.0% (3/5) |");
                softly.assertThat(리포트).contains("| PROFANE | 0 | 2 | 0 | 1 |");
                softly.assertThat(리포트).contains("| 회차별 MISSING | 1 |");
                softly.assertThat(리포트).contains("| 시발점 | 오탐유발정상어 | CLEAN | FLAGGED | 사유 |");
            });
        }
    }

    @Test
    void 회차마다_판정이_달라진_닉네임_비율을_흔들림으로_잰다() {
        final AtomicInteger 호출 = new AtomicInteger();
        final NicknameAuditor 검열기 = nicknames -> {
            final boolean 첫_호출 = 호출.getAndIncrement() == 0;
            return List.of(
                    판정("흔들리는닉", 첫_호출 ? NicknameAuditStatus.CLEAN : NicknameAuditStatus.PENDING),
                    판정("고정닉", NicknameAuditStatus.CLEAN));
        };

        final NicknameAuditEvaluation 결과 = evaluator.evaluate(List.of(경계("흔들리는닉"), 정상("고정닉", "일반닉네임")), 검열기, 100, 3);

        assertThat(결과.instability()).isEqualTo(new Rate(1, 2));
    }

    @Test
    void 판정을_못_받은_회차는_흔들림_계산에서_뺀다() {
        final AtomicInteger 호출 = new AtomicInteger();
        // 둘째 회차는 호출이 실패하고, 셋째 회차는 첫 회차와 같은 판정을 준다
        final NicknameAuditor 검열기 = nicknames -> {
            if (호출.getAndIncrement() == 1) {
                throw new IllegalStateException("할당량 초과");
            }
            return List.of(판정("고정닉", NicknameAuditStatus.CLEAN));
        };

        final NicknameAuditEvaluation 결과 = evaluator.evaluate(List.of(정상("고정닉", "일반닉네임")), 검열기, 100, 3);

        assertSoftly(softly -> {
            softly.assertThat(결과.instability()).isEqualTo(new Rate(0, 1));
            softly.assertThat(결과.missingByRun()).containsExactly(0, 1, 0);
        });
    }

    @Test
    void 두_회차_넘게_판정을_받지_못한_닉네임은_흔들림_분모에서_뺀다() {
        final AtomicInteger 호출 = new AtomicInteger();
        final NicknameAuditor 검열기 = nicknames -> {
            if (호출.getAndIncrement() > 0) {
                throw new IllegalStateException("할당량 초과");
            }
            return List.of(판정("고정닉", NicknameAuditStatus.CLEAN));
        };

        final NicknameAuditEvaluation 결과 = evaluator.evaluate(List.of(정상("고정닉", "일반닉네임")), 검열기, 100, 3);

        assertThat(결과.instability().toString()).isEqualTo("n/a");
    }

    @Test
    void 검열기가_응답_누락을_메운_PENDING은_MISSING으로_센다() {
        // 운영 검열기는 응답에서 빠진 닉네임을 신뢰도 UNKNOWN의 PENDING으로 메운다. 모델이 내린 판정이 아니다
        final NicknameAuditor 검열기 = nicknames -> List.of(
                new NicknameAuditResult("빠진닉", NicknameAuditStatus.PENDING, AiConfidence.UNKNOWN, "응답 항목 수 불일치"),
                new NicknameAuditResult("애매닉", NicknameAuditStatus.PENDING, AiConfidence.of(0.6), "애매함"));

        final NicknameAuditEvaluation 결과 = evaluator.evaluate(List.of(경계("빠진닉"), 경계("애매닉")), 검열기, 100, 1);

        assertThat(결과.confusion().get(Expected.AMBIGUOUS))
                .containsEntry(Predicted.MISSING, 1)
                .containsEntry(Predicted.PENDING, 1);
    }

    @Nested
    class 호출_기록 {

        private static final List<GoldenItem> 항목 = List.of(정상("첫째닉", "일반닉네임"), 정상("둘째닉", "일반닉네임"), 정상("셋째닉", "일반닉네임"));

        /** 첫째닉이 실린 배치만 실패한다. */
        private static final NicknameAuditor 첫_배치만_실패하는_검열기 = nicknames -> {
            if (nicknames.contains("첫째닉")) {
                throw new IllegalStateException("배치  실패", new IllegalArgumentException("504\ttimeout"));
            }
            return nicknames.stream().map(n -> 판정(n, NicknameAuditStatus.CLEAN)).toList();
        };

        @Test
        void 호출마다_회차와_배치와_성공_여부를_남긴다() {
            final NicknameAuditEvaluation 결과 = evaluator.evaluate(항목, 첫_배치만_실패하는_검열기, 2, 2);

            // 회차마다 배치 2개
            assertThat(결과.calls())
                    .extracting(
                            CallRecord::run,
                            CallRecord::batch,
                            CallRecord::size,
                            CallRecord::succeeded,
                            CallRecord::failure)
                    .containsExactly(
                            tuple(1, 1, 2, false, "IllegalArgumentException: 504 timeout"),
                            tuple(1, 2, 1, true, ""),
                            tuple(2, 1, 2, false, "IllegalArgumentException: 504 timeout"),
                            tuple(2, 2, 1, true, ""));
        }

        @Test
        void 응답_시간은_성공한_호출만으로_내고_실패는_따로_센다() {
            final TokenUsage 사용량 = new TokenUsage(1, 2, 3, 4, 10);
            final NicknameAuditEvaluation 측정 = evaluator.evaluate(항목, 첫_배치만_실패하는_검열기, 2, 1);
            // 실패한 호출이 훨씬 오래 걸린 것으로 바꿔 끼운다. 평균과 최대에 섞이면 값이 달라진다
            final NicknameAuditEvaluation 결과 = 호출_기록을_바꾼(
                    측정,
                    List.of(
                            new CallRecord(1, 1, 2, Duration.ofSeconds(120), TokenUsage.NONE, "타임아웃"),
                            new CallRecord(1, 2, 1, Duration.ofSeconds(10), 사용량, ""),
                            new CallRecord(1, 3, 1, Duration.ofSeconds(20), 사용량, "")));

            assertThat(결과.toMarkdown("대상"))
                    .contains("| 성공한 호출 응답 시간 (평균 / 최대) | 15.0s / 20.0s |")
                    .contains("| 실패한 호출 | 33.3% (1/3) |");
        }

        @Test
        void 성공한_호출에만_토큰_사용량을_붙인다() {
            final AtomicInteger 조회_횟수 = new AtomicInteger();

            final NicknameAuditEvaluation 결과 = evaluator.evaluate(
                    항목, 첫_배치만_실패하는_검열기, 2, 1, null, () -> new TokenUsage(100, 20, 30, 5, 조회_횟수.incrementAndGet()));

            assertSoftly(softly -> {
                softly.assertThat(결과.calls())
                        .extracting(CallRecord::usage)
                        .containsExactly(TokenUsage.NONE, new TokenUsage(100, 20, 30, 5, 1));
                softly.assertThat(결과.toCallsTsv().lines())
                        .containsExactly(
                                "run\tbatch\tsize\tsucceeded\tlatency_ms\tprompt_tokens\tcandidates_tokens"
                                        + "\tcached_tokens\tthoughts_tokens\ttotal_tokens\tfailure",
                                "1\t1\t2\tfalse\t%d\t0\t0\t0\t0\t0\tIllegalArgumentException: 504 timeout"
                                        .formatted(결과.calls().get(0).latency().toMillis()),
                                "1\t2\t1\ttrue\t%d\t100\t20\t30\t5\t1\t"
                                        .formatted(결과.calls().get(1).latency().toMillis()));
            });
        }

        private static NicknameAuditEvaluation 호출_기록을_바꾼(NicknameAuditEvaluation 원본, List<CallRecord> calls) {
            return new NicknameAuditEvaluation(
                    원본.itemCount(),
                    원본.repeats(),
                    원본.batchSize(),
                    원본.missingByRun(),
                    원본.shuffleSeed(),
                    calls,
                    원본.overall(),
                    원본.byCategory(),
                    원본.confusion(),
                    원본.cleanFalsePositive(),
                    원본.positiveMiss(),
                    원본.ambiguousCapture(),
                    원본.termAccuracy(),
                    원본.instability(),
                    원본.mismatches(),
                    원본.rows());
        }
    }

    @Nested
    class 순서_섞기 {

        private static final List<GoldenItem> 항목 =
                IntStream.rangeClosed(1, 20).mapToObj(i -> 정상("닉" + i, "일반닉네임")).toList();

        private List<List<String>> 회차별_보낸_순서(Long 시드) {
            final List<String> 보낸_닉네임 = new ArrayList<>();
            final NicknameAuditor 검열기 = nicknames -> {
                보낸_닉네임.addAll(nicknames);
                return nicknames.stream()
                        .map(n -> 판정(n, NicknameAuditStatus.CLEAN))
                        .toList();
            };
            evaluator.evaluate(항목, 검열기, 7, 2, 시드, () -> TokenUsage.NONE);
            return List.of(보낸_닉네임.subList(0, 20), 보낸_닉네임.subList(20, 40));
        }

        @Test
        void 시드가_없으면_골든셋_순서_그대로_보낸다() {
            final List<String> 원래_순서 = 항목.stream().map(GoldenItem::nickname).toList();

            assertThat(회차별_보낸_순서(null)).containsExactly(원래_순서, 원래_순서);
        }

        @Test
        void 시드가_있으면_회차마다_다른_순서로_섞고_닉네임은_빠뜨리지_않는다() {
            final List<String> 원래_순서 = 항목.stream().map(GoldenItem::nickname).toList();

            final List<List<String>> 순서 = 회차별_보낸_순서(1907L);

            assertSoftly(softly -> {
                softly.assertThat(순서.get(0)).isNotEqualTo(원래_순서).containsExactlyInAnyOrderElementsOf(원래_순서);
                softly.assertThat(순서.get(1)).isNotEqualTo(원래_순서).containsExactlyInAnyOrderElementsOf(원래_순서);
                softly.assertThat(순서.get(0)).isNotEqualTo(순서.get(1));
            });
        }

        @Test
        void 같은_시드는_같은_순서를_다시_만든다() {
            assertThat(회차별_보낸_순서(1907L)).isEqualTo(회차별_보낸_순서(1907L));
            assertThat(회차별_보낸_순서(1907L)).isNotEqualTo(회차별_보낸_순서(1908L));
        }

        @Test
        void 회차_n은_시드에_n을_더한_값으로_섞는다() {
            // 시드 10의 2회차와 시드 11의 1회차는 둘 다 12로 섞는다
            assertThat(회차별_보낸_순서(10L).get(1)).isEqualTo(회차별_보낸_순서(11L).get(0));
        }

        @Test
        void 섞은_순서대로_행에_위치와_배치를_적는다() {
            final NicknameAuditor 검열기 = nicknames -> nicknames.stream()
                    .map(n -> 판정(n, NicknameAuditStatus.CLEAN))
                    .toList();

            final NicknameAuditEvaluation 결과 = evaluator.evaluate(항목, 검열기, 7, 1, 1907L, () -> TokenUsage.NONE);

            assertSoftly(softly -> {
                softly.assertThat(결과.shuffleSeed()).isEqualTo(1907L);
                softly.assertThat(결과.rows())
                        .extracting(RunRow::nickname)
                        .isEqualTo(회차별_보낸_순서(1907L).get(0));
                softly.assertThat(결과.rows())
                        .extracting(RunRow::position)
                        .isEqualTo(IntStream.rangeClosed(1, 20).boxed().toList());
                softly.assertThat(결과.rows())
                        .extracting(RunRow::batch)
                        .startsWith(1, 1, 1, 1, 1, 1, 1, 2)
                        .endsWith(2, 3, 3, 3, 3, 3, 3);
                softly.assertThat(결과.toMarkdown("대상")).contains("- 닉네임 순서: 회차마다 섞음(시드 1907 + 회차)");
            });
        }
    }

    @Test
    void 배치_크기로_나눠_부르고_실패한_배치는_MISSING으로_센다() {
        final List<List<String>> 호출된_배치 = new ArrayList<>();
        final NicknameAuditor 검열기 = nicknames -> {
            호출된_배치.add(List.copyOf(nicknames));
            if (nicknames.contains("셋째닉")) {
                throw new IllegalStateException("호출 실패");
            }
            return nicknames.stream().map(n -> 판정(n, NicknameAuditStatus.CLEAN)).toList();
        };
        final List<GoldenItem> 항목 = List.of(정상("첫째닉", "일반닉네임"), 정상("둘째닉", "일반닉네임"), 정상("셋째닉", "일반닉네임"));

        final NicknameAuditEvaluation 결과 = evaluator.evaluate(항목, 검열기, 2, 1);

        assertSoftly(softly -> {
            softly.assertThat(호출된_배치).containsExactly(List.of("첫째닉", "둘째닉"), List.of("셋째닉"));
            softly.assertThat(결과.confusion().get(Expected.CLEAN))
                    .containsEntry(Predicted.CLEAN, 2)
                    .containsEntry(Predicted.MISSING, 1);
        });
    }

    @Test
    void 요청하지_않은_닉네임의_결과는_버려_뒤_배치_항목을_가로채지_않는다() {
        // 첫 배치 응답에 둘째 배치 닉네임 판정이 섞여 온다
        final NicknameAuditor 검열기 = nicknames -> nicknames.contains("첫째닉")
                ? List.of(판정("첫째닉", NicknameAuditStatus.CLEAN), 판정("둘째닉", NicknameAuditStatus.FLAGGED))
                : List.of(판정("둘째닉", NicknameAuditStatus.CLEAN));

        final NicknameAuditEvaluation 결과 =
                evaluator.evaluate(List.of(정상("첫째닉", "일반닉네임"), 정상("둘째닉", "일반닉네임")), 검열기, 1, 1);

        assertThat(결과.confusion().get(Expected.CLEAN)).containsEntry(Predicted.CLEAN, 2);
    }

    @Test
    void 회차마다_닉네임별_판정과_모델이_낸_원값을_행으로_남긴다() {
        // 넷째닉은 응답에서 빠지고, 다섯째닉은 검열기가 누락을 메운 값이다
        final NicknameAuditor 검열기 = 고정_검열기(Map.of(
                "첫째닉", NicknameAuditResult.of("첫째닉", true, 0.95, "욕설\t포함\n확실", 0.85),
                "둘째닉", NicknameAuditResult.of("둘째닉", true, 0.6, "애매", 0.85),
                "셋째닉", NicknameAuditResult.of("셋째닉", false, 0.999, "정상", 0.85),
                "다섯째닉", new NicknameAuditResult("다섯째닉", NicknameAuditStatus.PENDING, AiConfidence.UNKNOWN, "응답 누락")));
        final List<GoldenItem> 항목 = List.of(
                욕설("첫째닉", "직접욕설", "첫째"), 경계("둘째닉"), 정상("셋째닉", "일반닉네임"), 정상("넷째닉", "일반닉네임"), 정상("다섯째닉", "일반닉네임"));

        final NicknameAuditEvaluation 결과 = evaluator.evaluate(항목, 검열기, 3, 2);

        assertSoftly(softly -> {
            softly.assertThat(결과.rows())
                    .extracting(
                            RunRow::nickname,
                            RunRow::run,
                            RunRow::batch,
                            RunRow::position,
                            RunRow::predicted,
                            RunRow::flagged,
                            RunRow::confidence)
                    .containsExactly(
                            tuple("첫째닉", 1, 1, 1, Predicted.FLAGGED, "true", "0.95"),
                            tuple("둘째닉", 1, 1, 2, Predicted.PENDING, "true", "0.60"),
                            tuple("셋째닉", 1, 1, 3, Predicted.CLEAN, "false", "1.00"),
                            tuple("넷째닉", 1, 2, 4, Predicted.MISSING, "", ""),
                            tuple("다섯째닉", 1, 2, 5, Predicted.MISSING, "", ""),
                            tuple("첫째닉", 2, 1, 1, Predicted.FLAGGED, "true", "0.95"),
                            tuple("둘째닉", 2, 1, 2, Predicted.PENDING, "true", "0.60"),
                            tuple("셋째닉", 2, 1, 3, Predicted.CLEAN, "false", "1.00"),
                            tuple("넷째닉", 2, 2, 4, Predicted.MISSING, "", ""),
                            tuple("다섯째닉", 2, 2, 5, Predicted.MISSING, "", ""));
            softly.assertThat(결과.toRowsTsv().lines().findFirst())
                    .contains("nickname\texpected\tcategory\trun\tbatch\tposition\tpredicted\tflagged\tconfidence"
                            + "\tbatch_latency_ms\treason");
            // 사유의 탭과 줄바꿈이 열과 행을 깨지 않는다
            softly.assertThat(결과.toRowsTsv().lines())
                    .hasSize(11)
                    .allSatisfy(line -> assertThat(line.split("\t", -1)).hasSize(11))
                    .anySatisfy(line -> assertThat(line)
                            .startsWith("첫째닉\tPROFANE\t직접욕설\t1\t1\t1\tFLAGGED\ttrue\t0.95\t")
                            .endsWith("\t욕설 포함 확실"))
                    .anySatisfy(line -> assertThat(line).startsWith("넷째닉\tCLEAN\t일반닉네임\t1\t2\t4\tMISSING\t\t\t"));
        });
    }

    @Test
    void 경계_포착률은_PENDING으로_보낸_것만_센다() {
        final NicknameAuditor 검열기 = 고정_검열기(Map.of(
                "미친존재감", 판정("미친존재감", NicknameAuditStatus.PENDING),
                "개쩌는형", 판정("개쩌는형", NicknameAuditStatus.FLAGGED),
                "돌아이", 판정("돌아이", NicknameAuditStatus.CLEAN)));

        final NicknameAuditEvaluation 결과 = evaluator.evaluate(List.of(경계("미친존재감"), 경계("개쩌는형"), 경계("돌아이")), 검열기, 100, 1);

        assertThat(결과.ambiguousCapture()).isEqualTo(new Rate(1, 3));
    }

    @Test
    void 흔들림을_뺀_지표는_첫_회차_판정으로_센다() {
        // 첫 회차는 CLEAN, 둘째 회차는 FLAGGED
        final AtomicInteger 호출 = new AtomicInteger();
        final NicknameAuditor 검열기 = nicknames -> List.of(
                판정("용감한호랑이", 호출.getAndIncrement() == 0 ? NicknameAuditStatus.CLEAN : NicknameAuditStatus.FLAGGED));

        final NicknameAuditEvaluation 결과 = evaluator.evaluate(List.of(정상("용감한호랑이", "일반닉네임")), 검열기, 100, 2);

        assertThat(결과.cleanFalsePositive()).isEqualTo(new Rate(0, 1));
    }

    @Test
    void 응답에_같은_닉네임이_두_번_오면_앞선_판정을_쓴다() {
        final NicknameAuditor 검열기 = nicknames ->
                List.of(판정("용감한호랑이", NicknameAuditStatus.CLEAN), 판정("용감한호랑이", NicknameAuditStatus.FLAGGED));

        final NicknameAuditEvaluation 결과 = evaluator.evaluate(List.of(정상("용감한호랑이", "일반닉네임")), 검열기, 100, 1);

        assertThat(결과.confusion().get(Expected.CLEAN)).containsEntry(Predicted.CLEAN, 1);
    }

    @Test
    void 골든셋_CSV는_평가에_쓸_수_있는_형태다() {
        final List<GoldenItem> 골든셋 = NicknameGoldenSetFixture.골든셋();
        final TextNormalizer normalizer = new TextNormalizer();

        assertSoftly(softly -> {
            softly.assertThat(골든셋).extracting(GoldenItem::nickname).doesNotHaveDuplicates();
            // 닉네임 칼럼이 10자다
            softly.assertThat(골든셋)
                    .allSatisfy(item -> assertThat(item.nickname()).hasSizeBetween(1, 10));
            softly.assertThat(골든셋.stream().filter(item -> item.expected() == Expected.CLEAN))
                    .hasSizeGreaterThanOrEqualTo(골든셋.size() / 2);
            // 정답 조각이 채택 규칙을 못 통과하면 조각 정확도가 늘 틀린다
            softly.assertThat(골든셋.stream().filter(item -> item.expected().isPositive()))
                    .allSatisfy(item -> assertThat(item.expectedTerms())
                            .isNotEmpty()
                            .allSatisfy(term -> {
                                final String normalized = normalizer.normalize(term);
                                assertThat(normalized).hasSizeGreaterThanOrEqualTo(조각_최소_길이);
                                assertThat(normalizer.normalize(item.nickname()))
                                        .contains(normalized);
                            }));
        });
    }
}
