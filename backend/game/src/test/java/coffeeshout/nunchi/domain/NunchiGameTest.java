package coffeeshout.nunchi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.minigame.domain.MiniGameResult;
import java.time.Instant;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NunchiGameTest {

    private static final long WINDOW_MILLIS = 300L;
    private static final Instant T0 = Instant.parse("2026-06-21T00:00:00Z");

    private final Gamer 일 = Gamer.of("일", null);
    private final Gamer 이 = Gamer.of("이", null);
    private final Gamer 삼 = Gamer.of("삼", null);
    private final Gamer 사 = Gamer.of("사", null);

    private NunchiGame game;

    @BeforeEach
    void setUp() {
        game = new NunchiGame(WINDOW_MILLIS);
        game.setUp(List.of(일, 이, 삼, 사));
        game.startPlaying(); // DESCRIPTION → PLAYING: 이하 입력 테스트는 입력 수락 상태를 전제로 한다
    }

    private NunchiTier tierOf(Gamer gamer) {
        return ((NunchiScore) game.getScores().get(gamer)).getTier();
    }

    @Nested
    class 초기화_테스트 {

        @Test
        void 셋업하면_DESCRIPTION_1번으로_시작한다() {
            // 공유 setUp의 startPlaying을 거치지 않은 갓 셋업한 게임의 초기 상태를 본다
            final NunchiGame fresh = new NunchiGame(WINDOW_MILLIS);
            fresh.setUp(List.of(일, 이, 삼, 사));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(fresh.getState()).isEqualTo(NunchiState.DESCRIPTION);
                softly.assertThat(fresh.getCurrentNumber()).isEqualTo(1);
                softly.assertThat(fresh.isFinished()).isFalse();
            });
        }

        @Test
        void DESCRIPTION_중_press는_IGNORED다() {
            final NunchiGame fresh = new NunchiGame(WINDOW_MILLIS);
            fresh.setUp(List.of(일, 이, 삼, 사));

            // 규칙 설명 중에는 아직 입력을 받지 않는다 — 예외가 아니라 IGNORED로 흡수(결정 1)
            final PressResult result = fresh.press(일, T0);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.outcome()).isEqualTo(PressOutcome.IGNORED);
                softly.assertThat(fresh.getScores()).doesNotContainKey(일);
                softly.assertThat(fresh.getCurrentNumber()).isEqualTo(1);
            });
        }

        @Test
        void startPlaying하면_PLAYING으로_전이해_입력을_받는다() {
            final NunchiGame fresh = new NunchiGame(WINDOW_MILLIS);
            fresh.setUp(List.of(일, 이, 삼, 사));

            fresh.startPlaying();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(fresh.getState()).isEqualTo(NunchiState.PLAYING);
                softly.assertThat(fresh.press(일, T0).outcome()).isEqualTo(PressOutcome.STOOD);
            });
        }
    }

    @Nested
    class findByName_테스트 {

        @Test
        void 닉네임으로_setUp에_주입된_원본_Gamer_인스턴스를_돌려준다() {
            // when
            final Gamer found = game.findByName("일");

            // then — 새 인스턴스가 아니라 주입된 원본이어야 점수맵 키와 매칭된다
            assertThat(found).isSameAs(일);
        }

        @Test
        void 없는_닉네임이면_예외를_던진다() {
            assertThat(catchThrowable(() -> game.findByName("없는사람")))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    class 단독_입력_테스트 {

        @Test
        void 첫_press는_즉시_STOOD다() {
            // when
            final PressResult result = game.press(일, T0);

            // then — 낙관적 즉시 일어서기, 번호는 아직 전진하지 않음
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.outcome()).isEqualTo(PressOutcome.STOOD);
                softly.assertThat(result.number()).isEqualTo(1);
                softly.assertThat(game.getCurrentNumber()).isEqualTo(1);
            });
        }

        @Test
        void 윈도우가_닫히면_solo로_확정되고_번호가_전진한다() {
            // given
            game.press(일, T0);

            // when
            game.closeWindow();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tierOf(일)).isEqualTo(NunchiTier.SOLO);
                softly.assertThat(game.getCurrentNumber()).isEqualTo(2);
                softly.assertThat(game.getState()).isEqualTo(NunchiState.PLAYING);
            });
        }

        @Test
        void 윈도우를_지난_press가_오면_직전_입력이_solo로_확정되고_새_press가_다음_번호를_연다() {
            // given
            game.press(일, T0);

            // when — closeWindow 없이 윈도우를 넘긴 시점에 다른 사람이 누름
            final PressResult result = game.press(이, T0.plusMillis(WINDOW_MILLIS + 100));

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(tierOf(일)).isEqualTo(NunchiTier.SOLO);
                softly.assertThat(result.outcome()).isEqualTo(PressOutcome.STOOD);
                softly.assertThat(result.number()).isEqualTo(2);
                softly.assertThat(game.getCurrentNumber()).isEqualTo(2);
            });
        }
    }

    @Nested
    class 충돌_테스트 {

        @Test
        void 윈도우_안에_둘이_누르면_충돌하고_쿨다운에_들어간다() {
            // given
            game.press(일, T0);

            // when
            final PressResult result = game.press(이, T0.plusMillis(200));

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.outcome()).isEqualTo(PressOutcome.COLLIDED);
                softly.assertThat(result.collidedGroup()).containsExactlyInAnyOrder(일, 이);
                softly.assertThat(game.getState()).isEqualTo(NunchiState.COLLISION_COOLDOWN);
                softly.assertThat(game.getCurrentNumber()).isEqualTo(1); // 카운터 reset(전진 안 함)
                softly.assertThat(tierOf(일)).isEqualTo(NunchiTier.COLLISION);
                softly.assertThat(tierOf(이)).isEqualTo(NunchiTier.COLLISION);
            });
        }

        @Test
        void 같은_충돌_그룹은_동점이다() {
            // given
            game.press(일, T0);
            game.press(이, T0.plusMillis(200));

            // when & then — 한 그룹은 동일 점수
            assertThat(game.getScores().get(일)).isEqualTo(game.getScores().get(이));
        }

        @Test
        void 윈도우_안의_세번째_늦은_press도_같은_충돌_그룹에_합류한다() {
            // given — 쿨다운 중이라도 instant가 윈도우(anchor+300ms) 안이면 합류(ADR N2)
            game.press(일, T0);
            game.press(이, T0.plusMillis(150));

            // when
            final PressResult result = game.press(삼, T0.plusMillis(250));

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.outcome()).isEqualTo(PressOutcome.COLLIDED);
                softly.assertThat(result.collidedGroup()).containsExactlyInAnyOrder(일, 이, 삼);
                softly.assertThat(game.getScores().get(삼)).isEqualTo(game.getScores().get(일));
            });
        }

        @Test
        void 쿨다운_중_윈도우_밖_입력은_무시된다() {
            // given
            game.press(일, T0);
            game.press(이, T0.plusMillis(200));

            // when — 윈도우(300ms)를 벗어난 시점의 입력
            final PressResult result = game.press(삼, T0.plusMillis(500));

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.outcome()).isEqualTo(PressOutcome.IGNORED);
                softly.assertThat(game.getScores()).doesNotContainKey(삼);
            });
        }

        @Test
        void 충돌한_사람의_재입력은_무시된다() {
            // given
            game.press(일, T0);
            game.press(이, T0.plusMillis(200));

            // when
            final PressResult result = game.press(일, T0.plusMillis(250));

            // then — 1인 1press, 충돌자는 영구 OUT
            assertThat(result.outcome()).isEqualTo(PressOutcome.IGNORED);
        }

        @Test
        void 쿨다운이_끝나면_남은_사람이_그_번호를_다시_차지한다() {
            // given — 일·이 충돌(1번), 쿨다운 종료
            game.press(일, T0);
            game.press(이, T0.plusMillis(200));
            game.endCooldown();

            // when — 아직 안 누른 삼이 1번을 차지
            final PressResult stood = game.press(삼, T0.plusMillis(1_000));
            game.closeWindow();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(game.getState()).isEqualTo(NunchiState.PLAYING);
                softly.assertThat(stood.outcome()).isEqualTo(PressOutcome.STOOD);
                softly.assertThat(tierOf(삼)).isEqualTo(NunchiTier.SOLO);
                softly.assertThat(game.getCurrentNumber()).isEqualTo(2);
            });
        }
    }

    @Nested
    class 종료_테스트 {

        @Test
        void 타임아웃되면_미입력자가_MISS로_확정되고_DONE이_된다() {
            // given — 일만 단독 입력, 나머지는 미입력
            game.press(일, T0);
            game.closeWindow();

            // when
            final List<Gamer> missed = game.finishByTimeout();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(game.isFinished()).isTrue();
                softly.assertThat(missed).containsExactlyInAnyOrder(이, 삼, 사);
                softly.assertThat(tierOf(이)).isEqualTo(NunchiTier.MISS);
            });
        }

        @Test
        void 최종_랭킹은_정상_충돌_미입력_3계층으로_매겨진다() {
            // given — 삼·사 충돌(1번) → 일 단독(1번 재차지) → 이 미입력
            game.press(삼, T0);
            game.press(사, T0.plusMillis(100));   // 삼·사 충돌, 쿨다운
            game.endCooldown();
            game.press(일, T0.plusMillis(1_000));  // 일 단독으로 1번 차지
            game.closeWindow();
            game.finishByTimeout();                // 이 미입력 → MISS

            // when
            final MiniGameResult result = game.getResult();

            // then — 일(정상)=1, 삼·사(충돌, 동점)=2, 이(미입력)=4
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getPlayerRank(일)).isEqualTo(1);
                softly.assertThat(result.getPlayerRank(삼)).isEqualTo(2);
                softly.assertThat(result.getPlayerRank(사)).isEqualTo(2);
                softly.assertThat(result.getPlayerRank(이)).isEqualTo(4);
            });
        }

        @Test
        void 전원_입력을_마치면_isAllPressed가_참이다() {
            // given — 넷 다 단독으로 순서대로 입력
            game.press(일, T0);
            game.closeWindow();
            game.press(이, T0.plusMillis(1_000));
            game.closeWindow();
            game.press(삼, T0.plusMillis(2_000));
            game.closeWindow();
            game.press(사, T0.plusMillis(3_000)); // 마지막은 pending

            // when & then
            assertThat(game.isAllPressed()).isTrue();
        }
    }
}
