package coffeeshout.profanity.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NicknameAuditResultTest {

    private static final double THRESHOLD = 0.8;

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
}
