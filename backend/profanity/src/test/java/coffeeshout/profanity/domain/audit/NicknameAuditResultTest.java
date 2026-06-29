package coffeeshout.profanity.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.profanity.domain.TextNormalizer;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NicknameAuditResultTest {

    private static final double THRESHOLD = 0.8;
    private static final int MIN_TERM_LENGTH = 2;

    private final TextNormalizer textNormalizer = new TextNormalizer();

    private NicknameAuditResult flaggedWithTerms(String nickname, List<String> terms) {
        return new NicknameAuditResult(nickname, NicknameAuditStatus.FLAGGED, AiConfidence.of(0.95), "비속어 포함", terms);
    }

    @Nested
    class of_팩토리_메서드 {

        @Test
        void flagged가_false이면_CLEAN으로_결정된다() {
            final NicknameAuditResult result = NicknameAuditResult.of("용감한호랑이", false, 0.95, "일반 닉네임", THRESHOLD);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.status()).isEqualTo(NicknameAuditStatus.CLEAN);
                softly.assertThat(result.nickname()).isEqualTo("용감한호랑이");
                softly.assertThat(result.reason()).isEqualTo("일반 닉네임");
            });
        }

        @Test
        void flagged가_false이면_낮은_신뢰도에서도_CLEAN으로_결정된다() {
            final NicknameAuditResult result = NicknameAuditResult.of("용감한호랑이", false, 0.1, "일반 닉네임", THRESHOLD);

            assertThat(result.status()).isEqualTo(NicknameAuditStatus.CLEAN);
        }

        @Test
        void flagged이고_confidence가_임계값_이상이면_FLAGGED로_결정된다() {
            final NicknameAuditResult result = NicknameAuditResult.of("씨발", true, 0.95, "직접 욕설", THRESHOLD);

            assertThat(result.status()).isEqualTo(NicknameAuditStatus.FLAGGED);
        }

        @Test
        void flagged이고_confidence가_임계값_미만이면_PENDING으로_결정된다() {
            final NicknameAuditResult result = NicknameAuditResult.of("애매한닉네임", true, 0.5, "판단 불명확", THRESHOLD);

            assertThat(result.status()).isEqualTo(NicknameAuditStatus.PENDING);
        }

        @Test
        void flagged이고_confidence가_임계값과_정확히_같으면_FLAGGED로_결정된다() {
            final NicknameAuditResult result = NicknameAuditResult.of("경계닉네임", true, THRESHOLD, "경계값", THRESHOLD);

            assertThat(result.status()).isEqualTo(NicknameAuditStatus.FLAGGED);
        }

        @Test
        void confidence값이_결과에_포함된다() {
            final NicknameAuditResult result = NicknameAuditResult.of("닉네임", false, 0.92, "이유", THRESHOLD);

            assertThat(result.confidence()).isEqualTo(AiConfidence.of(0.92));
        }
    }

    @Nested
    class extractProfanityFragments {

        @Test
        void 닉네임의_부분문자열인_조각만_채택한다() {
            final NicknameAuditResult result = flaggedWithTerms("경찬이병신", List.of("병신", "핵상욕설"));

            final List<String> fragments = result.extractProfanityFragments(textNormalizer, MIN_TERM_LENGTH);

            assertThat(fragments).containsExactly("병신");
        }

        @Test
        void 여러_유효_조각을_모두_채택한다() {
            final NicknameAuditResult result = flaggedWithTerms("시발경찬이병신", List.of("시발", "병신"));

            final List<String> fragments = result.extractProfanityFragments(textNormalizer, MIN_TERM_LENGTH);

            assertThat(fragments).containsExactly("시발", "병신");
        }

        @Test
        void 정규화_후_최소_길이_미만_조각은_제외한다() {
            final NicknameAuditResult result = flaggedWithTerms("경찬이병신", List.of("병신", "이"));

            final List<String> fragments = result.extractProfanityFragments(textNormalizer, MIN_TERM_LENGTH);

            assertThat(fragments).containsExactly("병신");
        }

        @Test
        void 정규화_결과가_같은_조각은_첫_raw_형태로_한_번만_채택한다() {
            final NicknameAuditResult result = flaggedWithTerms("시1발놈", List.of("시1발", "시i발"));

            final List<String> fragments = result.extractProfanityFragments(textNormalizer, MIN_TERM_LENGTH);

            assertThat(fragments).containsExactly("시1발");
        }

        @Test
        void null이나_빈_조각은_무시한다() {
            final NicknameAuditResult result = flaggedWithTerms("경찬이병신", java.util.Arrays.asList("병신", null, "   "));

            final List<String> fragments = result.extractProfanityFragments(textNormalizer, MIN_TERM_LENGTH);

            assertThat(fragments).containsExactly("병신");
        }

        @Test
        void 유효한_조각이_없으면_빈_리스트를_반환한다() {
            final NicknameAuditResult result = flaggedWithTerms("씨발놈", List.of("닉네임에없는말"));

            final List<String> fragments = result.extractProfanityFragments(textNormalizer, MIN_TERM_LENGTH);

            assertThat(fragments).isEmpty();
        }
    }
}
