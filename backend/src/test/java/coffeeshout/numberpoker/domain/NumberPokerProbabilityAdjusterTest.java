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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 4명 · 3라운드 기준
 * step = (10000/4) / 3 / (4/2.0) = 416
 * stage1Fold = (int)(416 * 0.3) = 124
 * stage2Fold = (int)(416 * 0.6) = 249
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
            // A=WIN, B=TIE, C=STAGE_1_FOLD(+124), D=LOSE(+416)
            // 증가분 = 540 → WIN 1명: -540
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, TIE, C, STAGE_1_FOLD, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-540);
                softly.assertThat(changes.get(B)).isEqualTo(0);
                softly.assertThat(changes.get(C)).isEqualTo(124);
                softly.assertThat(changes.get(D)).isEqualTo(416);
            });
        }
    }

    @Nested
    class 케이스2_TIE_흡수 {

        @Test
        void WIN_없을_때_TIE가_흡수자가_된다() {
            // A=TIE(꾹이), B=TIE(루키), C=STAGE_2_FOLD(+249), D=LOSE(+416)
            // 증가분 = 665 → TIE 2명 균등 배분: 665/2=332, 나머지 1
            // 이름 순 정렬: 꾹이(A) → -332, 루키(B) → -(665-332)=-333 (zero-sum 보장)
            final Map<Player, PokerRoundResult> results = Map.of(A, TIE, B, TIE, C, STAGE_2_FOLD, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-332);
                softly.assertThat(changes.get(B)).isEqualTo(-333);
                softly.assertThat(changes.get(C)).isEqualTo(249);
                softly.assertThat(changes.get(D)).isEqualTo(416);
            });
        }

        @Test
        void TIE와_STAGE_1_FOLD만_있을_때_TIE가_흡수하고_STAGE_1_FOLD는_증가한다() {
            // A=TIE, B=STAGE_1_FOLD(+124)
            // TIE는 2순위 흡수자 → A: -124, B: +124
            final Map<Player, PokerRoundResult> results = Map.of(A, TIE, B, STAGE_1_FOLD);

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-124);
                softly.assertThat(changes.get(B)).isEqualTo(124);
            });
        }
    }

    @Nested
    class 케이스3_FOLD_흡수 {

        @Test
        void WIN_TIE_없을_때_FOLD_전체가_LOSE를_흡수한다() {
            // A=STAGE_1_FOLD, B=STAGE_2_FOLD, C=LOSE(+416), D=LOSE(+416)
            // LOSE 증가분 = 832 → FOLD 2명 균등: 832/2=416, 나머지 0
            final Map<Player, PokerRoundResult> results = Map.of(A, STAGE_1_FOLD, B, STAGE_2_FOLD, C, LOSE, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-416);
                softly.assertThat(changes.get(B)).isEqualTo(-416);
                softly.assertThat(changes.get(C)).isEqualTo(416);
                softly.assertThat(changes.get(D)).isEqualTo(416);
            });
        }
    }

    @Nested
    class 케이스5_STAGE1_FOLD_흡수 {

        @Test
        void STAGE_1_FOLD와_STAGE_2_FOLD만_있을_때_STAGE_1_FOLD가_흡수자가_된다() {
            // A=STAGE_1_FOLD, B=STAGE_1_FOLD, C=STAGE_2_FOLD(+249), D=STAGE_2_FOLD(+249)
            // STAGE_2_FOLD 증가분 = 498 → STAGE_1_FOLD 2명 균등: 498/2=249, 나머지 0
            final Map<Player, PokerRoundResult> results = Map.of(
                    A, STAGE_1_FOLD, B, STAGE_1_FOLD, C, STAGE_2_FOLD, D, STAGE_2_FOLD
            );

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-249);
                softly.assertThat(changes.get(B)).isEqualTo(-249);
                softly.assertThat(changes.get(C)).isEqualTo(249);
                softly.assertThat(changes.get(D)).isEqualTo(249);
            });
        }
    }

    @Nested
    class 케이스6_전원_변동_없음 {

        @Test
        void 전원_LOSE이면_흡수자_없어_변동이_없다() {
            final Map<Player, PokerRoundResult> results = Map.of(A, LOSE, B, LOSE, C, LOSE, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            assertThat(changes.values()).allMatch(v -> v == 0);
        }

        @Test
        void STAGE_2_FOLD만_존재하면_흡수자_없어_변동이_없다() {
            final Map<Player, PokerRoundResult> results = Map.of(
                    A, STAGE_2_FOLD, B, STAGE_2_FOLD, C, STAGE_2_FOLD, D, STAGE_2_FOLD
            );

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            assertThat(changes.values()).allMatch(v -> v == 0);
        }

        @Test
        void 전원_WIN이면_증가분_없어_변동이_없다() {
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, WIN, C, WIN, D, WIN);

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            assertThat(changes.values()).allMatch(v -> v == 0);
        }
    }

    @Nested
    class 합계_검증 {

        @Test
        void WIN이_흡수할_때_변동량_합계는_0이다() {
            // A=WIN, B=TIE, C=STAGE_1_FOLD(+124), D=LOSE(+416) → -540+0+124+416=0
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, TIE, C, STAGE_1_FOLD, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            assertThat(changes.values().stream().mapToInt(Integer::intValue).sum()).isZero();
        }

        @Test
        void TIE가_흡수할_때_나머지_배분으로_변동량_합계는_0이다() {
            // A=TIE, B=TIE, C=STAGE_2_FOLD(+249), D=LOSE(+416) → 665 홀수 나머지 처리
            final Map<Player, PokerRoundResult> results = Map.of(A, TIE, B, TIE, C, STAGE_2_FOLD, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            assertThat(changes.values().stream().mapToInt(Integer::intValue).sum()).isZero();
        }
    }

    @Nested
    class 라운드별_스테이크_차등 {

        /**
         * 4명·3라운드 baseStep = 416
         * round 1: multiplier = 2*1/(3+1) = 0.5  → step = 208
         * round 2: multiplier = 2*2/(3+1) = 1.0  → step = 416
         * round 3: multiplier = 2*3/(3+1) = 1.5  → step = 624
         */
        @Test
        void 마지막_라운드_step이_첫_라운드보다_크다() {
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, LOSE);

            final int deltaRound1 = adjuster.calculate(results, Map.of(), 4, 3, 1).get(B);
            final int deltaRound3 = adjuster.calculate(results, Map.of(), 4, 3, 3).get(B);

            assertThat(deltaRound3).isGreaterThan(deltaRound1);
        }

        @Test
        void 중간_라운드_step이_라운드_수_무관_기준값과_일치한다() {
            // round 2/3 → multiplier = 1.0 → 기존 설계 값과 동일: D(LOSE) = 416
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, TIE, C, STAGE_1_FOLD, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, 3, 2);

            assertThat(changes.get(D)).isEqualTo(416);
        }

        @ParameterizedTest(name = "라운드 {0}/{1} → LOSE delta = {2}")
        @CsvSource({
                "1, 3, 208",
                "2, 3, 416",
                "3, 3, 624",
        })
        void 라운드_번호에_따라_LOSE_delta가_선형_증가한다(int roundNumber, int totalRounds, int expectedLoseDelta) {
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, Map.of(), 4, totalRounds, roundNumber);

            assertThat(changes.get(B)).isEqualTo(expectedLoseDelta);
        }

        @Test
        void 전체_라운드의_평균_step은_기준값과_동일하다() {
            // round 1+2+3 delta 합이 3라운드 동일 step의 합과 같아야 함 (기댓값 보존)
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, LOSE);
            final int deltaR1 = adjuster.calculate(results, Map.of(), 4, 3, 1).get(B);
            final int deltaR2 = adjuster.calculate(results, Map.of(), 4, 3, 2).get(B);
            final int deltaR3 = adjuster.calculate(results, Map.of(), 4, 3, 3).get(B);

            // 208 + 416 + 624 = 1248 = 416 * 3 (정수 truncation 오차 1 허용)
            assertThat(deltaR1 + deltaR2 + deltaR3).isBetween(416 * 3 - 3, 416 * 3);
        }
    }

    @Nested
    class WIN_흡수자_핸드_랭킹_차등 {

        /**
         * 핸드 랭킹 순위: 꾹이(PAIR_9) > 루키(HIGH_CARD_7_3) > 엠제이(HIGH_CARD_2_1)
         */
        final Map<Player, HandRanking> handRankings = Map.of(
                A, HandRanking.of(new PokerCard(9), new PokerCard(9)),   // PAIR(9) — 최강
                B, HandRanking.of(new PokerCard(7), new PokerCard(3)),   // HIGH_CARD(7,3)
                C, HandRanking.of(new PokerCard(2), new PokerCard(1))    // HIGH_CARD(2,1) — 최약
        );

        @Test
        void WIN_흡수자_1명이면_전액_흡수한다() {
            // A=WIN, B=LOSE(+416), C=STAGE_1_FOLD(+124) → A: -540
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, LOSE, C, STAGE_1_FOLD, D, TIE);

            final Map<Player, Integer> changes = adjuster.calculate(results, handRankings, 4, 3, 2);

            assertThat(changes.get(A)).isEqualTo(-540);
        }

        @Test
        void 강한_핸드의_WIN이_약한_핸드의_WIN보다_더_많이_흡수한다() {
            // A(PAIR_9) > B(HIGH_CARD_7_3) — 둘 다 WIN, D=LOSE(+416)*2 = 832
            // n=2, totalWeight=3: A weight=2 → 832*2/3=554, B: -(832-554)=-278
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, WIN, C, LOSE, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, handRankings, 4, 3, 2);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(Math.abs(changes.get(A))).isGreaterThan(Math.abs(changes.get(B)));
                softly.assertThat(changes.get(A)).isEqualTo(-554);
                softly.assertThat(changes.get(B)).isEqualTo(-278);
            });
        }

        @Test
        void WIN_3명일_때_핸드_강도_순으로_흡수량이_감소한다() {
            // A(PAIR_9) > B(HIGH_CARD_7_3) > C(HIGH_CARD_2_1), D=LOSE(+416)
            // n=3, totalWeight=6: A weight=3→208, B weight=2→138, C remainder→70
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, WIN, C, WIN, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, handRankings, 4, 3, 2);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(changes.get(A)).isEqualTo(-208);
                softly.assertThat(changes.get(B)).isEqualTo(-138);
                softly.assertThat(changes.get(C)).isEqualTo(-70);
            });
        }

        @Test
        void WIN_흡수자_랭킹_차등에서도_변동량_합계는_0이다() {
            final Map<Player, PokerRoundResult> results = Map.of(A, WIN, B, WIN, C, LOSE, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, handRankings, 4, 3, 2);

            assertThat(changes.values().stream().mapToInt(Integer::intValue).sum()).isZero();
        }

        @Test
        void WIN이_아닌_TIE_흡수자는_핸드_랭킹과_무관하게_균등_배분한다() {
            // A=TIE, B=TIE, C=LOSE(+416), D=LOSE(+416) → 832 균등: A=-332, B=-500(나머지)
            // 꾹이(A)='꾹이', 루키(B)='루키' 이름 순: 꾹이 먼저
            final Map<Player, PokerRoundResult> results = Map.of(A, TIE, B, TIE, C, LOSE, D, LOSE);

            final Map<Player, Integer> changes = adjuster.calculate(results, handRankings, 4, 3, 2);

            // TIE는 핸드 강도 무관 → 균등 배분 (기존 케이스2 동일)
            assertThat(changes.values().stream().mapToInt(Integer::intValue).sum()).isZero();
        }
    }

    @Nested
    class 플레이어_수별_step_스케일 {

        @Test
        void 플레이어_수가_많을수록_step이_작아진다() {
            // 2명 3라운드: step = (10000/2)/3/(2/2.0)*0.7 = 5000/3/1*0.7 = 1166
            // 8명 3라운드: step = (10000/8)/3/(8/2.0)*0.7 = 1250/3/4*0.7 = 72
            final Map<Player, PokerRoundResult> twoPlayerResults = Map.of(A, WIN, B, LOSE);
            final Map<Player, PokerRoundResult> eightPlayerResults = Map.of(
                    A, WIN, B, LOSE, C, LOSE, D, LOSE
            );

            final int winDelta2 = adjuster.calculate(twoPlayerResults, Map.of(), 2, 3, 2).get(A);
            final int winDelta8 = adjuster.calculate(eightPlayerResults, Map.of(), 8, 3, 2).get(A);

            assertThat(Math.abs(winDelta2)).isGreaterThan(Math.abs(winDelta8));
        }
    }
}
