package coffeeshout.numberpoker.domain;

import static coffeeshout.numberpoker.domain.PokerRoundResult.LOSE;
import static coffeeshout.numberpoker.domain.PokerRoundResult.STAGE_1_FOLD;
import static coffeeshout.numberpoker.domain.PokerRoundResult.STAGE_2_FOLD;
import static coffeeshout.numberpoker.domain.PokerRoundResult.TIE;
import static coffeeshout.numberpoker.domain.PokerRoundResult.WIN;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.room.domain.player.Player;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * adjustmentStep=300, roundCount=3 → step=100
 * stage1FoldMultiplier=0.3, stage2FoldMultiplier=0.6
 * 케이스는 ADR 0002 확률 조정 공식 기준
 */
class NumberPokerProbabilityAdjusterTest {

    NumberPokerProbabilityAdjuster adjuster;

    Player A = PlayerFixture.호스트꾹이();
    Player B = PlayerFixture.게스트루키();
    Player C = PlayerFixture.게스트엠제이();
    Player D = PlayerFixture.게스트한스();

    @BeforeEach
    void setUp() {
        adjuster = new NumberPokerProbabilityAdjuster(0.3, 0.6);
    }

    @Nested
    class 케이스1_WIN_존재 {

        @Test
        void WIN이_증가분_전체를_흡수한다() {
            // A=WIN, B=TIE, C=STAGE_1_FOLD(+30), D=LOSE(+100)
            // 증가분 = 130 → WIN 1명: -130
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, TIE, C, STAGE_1_FOLD, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, 300, 3);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-130);
                softly.assertThat(changes.get(B)).isEqualTo(0);
                softly.assertThat(changes.get(C)).isEqualTo(30);
                softly.assertThat(changes.get(D)).isEqualTo(100);
            });
        }
    }

    @Nested
    class 케이스2_TIE_흡수 {

        @Test
        void WIN_없을_때_TIE가_흡수자가_된다() {
            // A=TIE, B=TIE, C=STAGE_2_FOLD(+60), D=LOSE(+100)
            // 증가분 = 160 → TIE 2명: 각 -80
            final Map<Player, PokerRoundResult> results = Map.of(A, TIE, B, TIE, C, STAGE_2_FOLD, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, 300, 3);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-80);
                softly.assertThat(changes.get(B)).isEqualTo(-80);
                softly.assertThat(changes.get(C)).isEqualTo(60);
                softly.assertThat(changes.get(D)).isEqualTo(100);
            });
        }

        @Test
        void TIE와_STAGE_1_FOLD만_있을_때_TIE가_흡수하고_STAGE_1_FOLD는_증가한다() {
            // A=TIE, B=STAGE_1_FOLD(+30)
            // TIE는 2순위 흡수자 → A: -30, B: +30
            final Map<Player, PokerRoundResult> results = Map.of(A, TIE, B, STAGE_1_FOLD);

            final Map<Player, Integer> changes = adjuster.calculate(results, 300, 3);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-30);
                softly.assertThat(changes.get(B)).isEqualTo(30);
            });
        }
    }

    @Nested
    class 케이스3_FOLD_흡수 {

        @Test
        void WIN_TIE_없을_때_FOLD_전체가_LOSE를_흡수한다() {
            // A=STAGE_1_FOLD, B=STAGE_2_FOLD, C=LOSE(+100), D=LOSE(+100)
            // LOSE 증가분 = 200 → FOLD 2명: 각 -100
            final Map<Player, PokerRoundResult> results = Map.of(A, STAGE_1_FOLD, B, STAGE_2_FOLD, C, LOSE, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, 300, 3);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-100);
                softly.assertThat(changes.get(B)).isEqualTo(-100);
                softly.assertThat(changes.get(C)).isEqualTo(100);
                softly.assertThat(changes.get(D)).isEqualTo(100);
            });
        }
    }

    @Nested
    class 케이스5_STAGE1_FOLD_흡수 {

        @Test
        void STAGE_1_FOLD와_STAGE_2_FOLD만_있을_때_STAGE_1_FOLD가_흡수자가_된다() {
            // A=STAGE_1_FOLD, B=STAGE_1_FOLD, C=STAGE_2_FOLD(+60), D=STAGE_2_FOLD(+60)
            // STAGE_2_FOLD 증가분 = 120 → STAGE_1_FOLD 2명: 각 -60
            final Map<Player, PokerRoundResult> results = Map.of(
                    A, STAGE_1_FOLD, B, STAGE_1_FOLD, C, STAGE_2_FOLD, D, STAGE_2_FOLD
            );

            final Map<Player, Integer> changes = adjuster.calculate(results, 300, 3);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-60);
                softly.assertThat(changes.get(B)).isEqualTo(-60);
                softly.assertThat(changes.get(C)).isEqualTo(60);
                softly.assertThat(changes.get(D)).isEqualTo(60);
            });
        }
    }

    @Nested
    class 케이스6_전원_변동_없음 {

        @Test
        void 전원_LOSE이면_흡수자_없어_변동이_없다() {
            final Map<Player, PokerRoundResult> results = Map.of(A, LOSE, B, LOSE, C, LOSE, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, 300, 3);

            assertThat(changes.values()).allMatch(v -> v == 0);
        }

        @Test
        void STAGE_2_FOLD만_존재하면_흡수자_없어_변동이_없다() {
            final Map<Player, PokerRoundResult> results = Map.of(
                    A, STAGE_2_FOLD, B, STAGE_2_FOLD, C, STAGE_2_FOLD, D, STAGE_2_FOLD
            );

            final Map<Player, Integer> changes = adjuster.calculate(results, 300, 3);

            assertThat(changes.values()).allMatch(v -> v == 0);
        }

        @Test
        void 전원_WIN이면_증가분_없어_변동이_없다() {
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, WIN, C, WIN, D, WIN);

            final Map<Player, Integer> changes = adjuster.calculate(results, 300, 3);

            assertThat(changes.values()).allMatch(v -> v == 0);
        }
    }

    @Nested
    class 합계_검증 {

        @Test
        void 클램핑_없으므로_변동량_합계는_항상_0이다() {
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, TIE, C, STAGE_1_FOLD, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, 300, 3);

            final int total = changes.values().stream().mapToInt(Integer::intValue).sum();
            assertThat(total).isEqualTo(0);
        }
    }
}
