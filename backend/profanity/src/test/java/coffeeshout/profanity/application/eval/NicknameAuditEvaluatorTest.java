package coffeeshout.profanity.application.eval;

import static coffeeshout.profanity.fixture.NicknameGoldenSetFixture.경계;
import static coffeeshout.profanity.fixture.NicknameGoldenSetFixture.욕설;
import static coffeeshout.profanity.fixture.NicknameGoldenSetFixture.우회;
import static coffeeshout.profanity.fixture.NicknameGoldenSetFixture.정상;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import coffeeshout.profanity.application.eval.GoldenItem.Expected;
import coffeeshout.profanity.application.eval.NicknameAuditEvaluation.Mismatch;
import coffeeshout.profanity.application.eval.NicknameAuditEvaluation.Predicted;
import coffeeshout.profanity.application.eval.NicknameAuditEvaluation.Rate;
import coffeeshout.profanity.application.eval.NicknameAuditEvaluation.Scores;
import coffeeshout.profanity.domain.TextNormalizer;
import coffeeshout.profanity.domain.audit.AiConfidence;
import coffeeshout.profanity.domain.audit.NicknameAuditResult;
import coffeeshout.profanity.domain.audit.NicknameAuditStatus;
import coffeeshout.profanity.domain.audit.NicknameAuditor;
import coffeeshout.profanity.fixture.NicknameGoldenSetFixture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NicknameAuditEvaluatorTest {

    private final NicknameAuditEvaluator evaluator = new NicknameAuditEvaluator(new TextNormalizer(), 2);

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
        void 경계를_뺀_정답으로_정밀도와_재현율을_잰다() {
            assertThat(결과.overall())
                    .isEqualTo(new Scores(new Rate(3, 5), new Rate(3, 5), new Rate(2, 3), new Rate(2, 5)));
        }

        @Test
        void 카테고리별로_따로_잰다() {
            assertSoftly(softly -> {
                softly.assertThat(결과.byCategory()).containsOnlyKeys("직접욕설", "초성", "특수문자삽입", "경계", "일반닉네임", "오탐유발정상어");
                softly.assertThat(결과.byCategory().get("직접욕설"))
                        .isEqualTo(new Scores(new Rate(2, 2), new Rate(2, 3), new Rate(2, 2), new Rate(2, 3)));
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
        void 오탐률_미탐률_경계_포착률을_잰다() {
            assertSoftly(softly -> {
                softly.assertThat(결과.cleanFalsePositive()).isEqualTo(new Rate(2, 3));
                softly.assertThat(결과.positiveMiss()).isEqualTo(new Rate(2, 5));
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
                                assertThat(normalized).hasSizeGreaterThanOrEqualTo(2);
                                assertThat(normalizer.normalize(item.nickname()))
                                        .contains(normalized);
                            }));
        });
    }
}
