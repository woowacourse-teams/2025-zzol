package coffeeshout.nunchi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NunchiScoreTest {

    @Nested
    class 계층_밴드_분리_테스트 {

        @Test
        void 정상은_충돌보다_좋고_충돌은_미입력보다_좋다() {
            // given
            final NunchiScore solo = NunchiScore.solo(2_000L);
            final NunchiScore collision = NunchiScore.collision(1_000L);
            final NunchiScore miss = NunchiScore.miss();

            // when & then — 오름차순(작을수록 좋음)에서 solo < collision < miss
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(solo.getValue()).isLessThan(collision.getValue());
                softly.assertThat(collision.getValue()).isLessThan(miss.getValue());
            });
        }

        @Test
        void 계층마다_tier가_노출된다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(NunchiScore.solo(1L).getTier()).isEqualTo(NunchiTier.SOLO);
                softly.assertThat(NunchiScore.collision(1L).getTier()).isEqualTo(NunchiTier.COLLISION);
                softly.assertThat(NunchiScore.miss().getTier()).isEqualTo(NunchiTier.MISS);
            });
        }
    }

    @Nested
    class 계층_내_정렬_테스트 {

        @Test
        void 정상은_먼저_누를수록_좋다() {
            // given
            final NunchiScore earlier = NunchiScore.solo(1_000L);
            final NunchiScore later = NunchiScore.solo(2_000L);

            // when & then — 먼저 누른 쪽이 더 작은 값(좋음)
            assertThat(earlier.compareTo(later)).isNegative();
        }

        @Test
        void 충돌은_늦게_충돌할수록_덜_나쁘다() {
            // given
            final NunchiScore earlierCollision = NunchiScore.collision(1_000L);
            final NunchiScore laterCollision = NunchiScore.collision(2_000L);

            // when & then — 늦게 충돌(2000)이 더 작은 값(덜 나쁨)
            assertThat(laterCollision.compareTo(earlierCollision)).isNegative();
        }
    }

    @Nested
    class 동점_판정_테스트 {

        @Test
        void 같은_충돌시각은_서로_동점이다() {
            // given — 한 충돌 그룹은 동일한 충돌 시각을 공유한다
            final NunchiScore a = NunchiScore.collision(1_000L);
            final NunchiScore b = NunchiScore.collision(1_000L);

            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(a).isEqualTo(b);
                softly.assertThat(a.compareTo(b)).isZero();
            });
        }

        @Test
        void 미입력은_서로_동점이다() {
            // when & then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(NunchiScore.miss()).isEqualTo(NunchiScore.miss());
                softly.assertThat(NunchiScore.miss().compareTo(NunchiScore.miss())).isZero();
            });
        }

        @Test
        void 충돌시각이_다르면_동점이_아니다() {
            assertThat(NunchiScore.collision(1_000L)).isNotEqualTo(NunchiScore.collision(2_000L));
        }
    }

    @Nested
    @DisplayName("ADR-0031 예시 재현 — MiniGameResult standard-competition 랭킹")
    class 종합_랭킹_테스트 {

        @Test
        void 정상_충돌그룹스택_미입력이_3계층으로_랭크된다() {
            // given — 발생 순서: A·B 먼저 충돌(t=100), D·E 나중 충돌(t=200), C 단독(t=300), F 미입력
            final Gamer a = Gamer.of("A", null);
            final Gamer b = Gamer.of("B", null);
            final Gamer c = Gamer.of("C", null);
            final Gamer d = Gamer.of("D", null);
            final Gamer e = Gamer.of("E", null);
            final Gamer f = Gamer.of("F", null);

            final Map<Gamer, MiniGameScore> scores = new LinkedHashMap<>();
            scores.put(a, NunchiScore.collision(100L));
            scores.put(b, NunchiScore.collision(100L));
            scores.put(d, NunchiScore.collision(200L));
            scores.put(e, NunchiScore.collision(200L));
            scores.put(c, NunchiScore.solo(300L));
            scores.put(f, NunchiScore.miss());

            // when
            final MiniGameResult result = MiniGameResult.fromAscending(scores);

            // then — C(1) > D·E(공동 2, 나중 충돌) > A·B(공동 4, 먼저 충돌) > F(6, 미입력)
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getPlayerRank(c)).isEqualTo(1);
                softly.assertThat(result.getPlayerRank(d)).isEqualTo(2);
                softly.assertThat(result.getPlayerRank(e)).isEqualTo(2);
                softly.assertThat(result.getPlayerRank(a)).isEqualTo(4);
                softly.assertThat(result.getPlayerRank(b)).isEqualTo(4);
                softly.assertThat(result.getPlayerRank(f)).isEqualTo(6);
            });
        }
    }
}
