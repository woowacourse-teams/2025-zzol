package coffeeshout.wormgame.domain;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import coffeeshout.fixture.GamerFixture;
import coffeeshout.gamecommon.Gamer;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WormGameTest {

    final List<Gamer> gamers = GamerFixture.꾹이_루키_엠제이_한스();
    final List<Gamer> twoGamers = gamers.subList(0, 2); // 꾹이, 루키

    /** 판정 시나리오용 규칙 — 무적·페인트 유예를 끄고 틱당 30u를 움직여 기하를 단순화한다. */
    WormGameRules collisionRules(int invincibleTicks, int wetPaintSkipSegments) {
        return new WormGameRules(
                50L,
                600.0,
                1.6,
                1200,
                Math.toRadians(200),
                0.7,
                220.0,
                200,
                1200,
                0.30,
                0.05,
                40.0,
                invincibleTicks,
                6.0,
                wetPaintSkipSegments,
                5);
    }

    void addVerticalTrail(Worm owner, double x, double fromY, double toY, double step) {
        owner.getTrail().clear(); // 스폰 포인트가 첫 세그먼트에 섞여 기하를 오염시키지 않도록
        for (double y = fromY; y <= toY; y += step) {
            owner.getTrail().add(x, y);
        }
    }

    @Test
    void 게임_시작을_위해_준비한다() {
        // given
        final WormGame game = new WormGame();

        // when
        game.setUp(gamers);

        // then
        assertThat(game.getState()).isEqualTo(WormGameState.DESCRIPTION);
        assertThat(game.getWorms().all())
                .hasSize(4)
                .allMatch(Worm::isAlive)
                .allMatch(worm -> worm.distanceFromCenter() < game.currentRadius());
    }

    @Nested
    class 충돌_판정 {

        @Test
        void 한_틱에_궤적을_관통해도_사망한다() {
            // given — 틱당 30u 이동, 판정 반경 6u: 이동 전후 점은 모두 궤적에서 15u 떨어져 있어
            // 점 대 점 판정이면 놓치는 기하다. 선분(swept) 판정의 회귀 테스트.
            final WormGame game = new WormGame(collisionRules(0, 0));
            game.setUp(twoGamers);
            game.updateState(WormGameState.PLAYING);
            final Worm mover = game.getWorms().findByName("꾹이");
            final Worm owner = game.getWorms().findByName("루키");
            mover.placeAt(0, 0, 0);
            owner.placeAt(0, 100, Math.PI / 2);
            addVerticalTrail(owner, 15, -50, 50, 10);

            // when
            game.tick();

            // then
            assertThat(mover.isAlive()).isFalse();
            assertThat(mover.getDeathTick()).isEqualTo(1);
            assertThat(owner.isAlive()).isTrue();
        }

        @Test
        void 무적_중에는_궤적을_통과한다() {
            // given
            final WormGame game = new WormGame(collisionRules(1000, 0));
            game.setUp(twoGamers);
            game.updateState(WormGameState.PLAYING);
            final Worm mover = game.getWorms().findByName("꾹이");
            final Worm owner = game.getWorms().findByName("루키");
            mover.placeAt(0, 0, 0);
            owner.placeAt(0, 100, Math.PI / 2);
            addVerticalTrail(owner, 15, -50, 50, 10);

            // when
            game.tick();

            // then
            assertThat(mover.isAlive()).isTrue();
        }

        @Test
        void 무적_중에도_경계를_벗어나면_죽는다() {
            // given — 2인 아레나 반지름 ≈ 155.6u. 경계는 무적 면제 대상이 아니다.
            final WormGame game = new WormGame(collisionRules(1000, 0));
            game.setUp(twoGamers);
            game.updateState(WormGameState.PLAYING);
            final Worm escaper = game.getWorms().findByName("꾹이");
            escaper.placeAt(150, 0, 0);

            // when
            game.tick();

            // then
            assertThat(escaper.isAlive()).isFalse();
            assertThat(escaper.getDeathTick()).isEqualTo(1);
        }

        @Test
        void 타인_궤적의_최신_구간은_판정에서_제외된다() {
            // given — "마르지 않은 페인트": 최신 3세그먼트뿐인 궤적은 통과한다.
            final WormGame game = new WormGame(collisionRules(0, 3));
            game.setUp(twoGamers);
            game.updateState(WormGameState.PLAYING);
            final Worm mover = game.getWorms().findByName("꾹이");
            final Worm owner = game.getWorms().findByName("루키");
            mover.placeAt(0, 0, 0);
            owner.placeAt(0, 100, Math.PI / 2);
            addVerticalTrail(owner, 15, -9, 9, 6); // 4점 = 3세그먼트 전부 최신

            // when
            game.tick();

            // then
            assertThat(mover.isAlive()).isTrue();
        }

        @Test
        void 타인_궤적의_오래된_구간에는_죽는다() {
            // given — 교차 지점(y=0)이 최신 3세그먼트(y 20~50) 밖의 오래된 구간이다.
            final WormGame game = new WormGame(collisionRules(0, 3));
            game.setUp(twoGamers);
            game.updateState(WormGameState.PLAYING);
            final Worm mover = game.getWorms().findByName("꾹이");
            final Worm owner = game.getWorms().findByName("루키");
            mover.placeAt(0, 0, 0);
            owner.placeAt(0, 100, Math.PI / 2);
            addVerticalTrail(owner, 15, -50, 50, 10);

            // when
            game.tick();

            // then
            assertThat(mover.isAlive()).isFalse();
        }
    }

    @Nested
    class 순위와_동점 {

        @Test
        void 같은_틱_동시_사망은_같은_점수로_공동_순위가_된다() {
            // given — 정면충돌: 둘 다 같은 틱에 죽는다.
            final WormGame game = new WormGame(collisionRules(0, 0));
            game.setUp(twoGamers);
            game.updateState(WormGameState.PLAYING);
            game.getWorms().findByName("꾹이").placeAt(0, 0, 0);
            game.getWorms().findByName("루키").placeAt(60, 0, Math.PI);

            // when
            game.tick();

            // then
            final var scores = game.getScores();
            assertThat(scores.values().stream().distinct()).hasSize(1);
            assertThat(game.getResult().getRank().values()).containsExactly(1, 1);
            assertThat(game.isRoundOver()).isTrue();
        }

        @Test
        void 생존자는_마지막_사망자보다_위_순위가_된다() {
            // given — 둘은 정면충돌로 죽고 하나(엠제이)는 살아남는다.
            final WormGame game = new WormGame(collisionRules(0, 0));
            game.setUp(gamers.subList(0, 3)); // 꾹이, 루키, 엠제이
            game.updateState(WormGameState.PLAYING);
            game.getWorms().findByName("꾹이").placeAt(0, 0, 0);
            game.getWorms().findByName("루키").placeAt(60, 0, Math.PI);
            game.getWorms().findByName("엠제이").placeAt(0, -100, Math.PI); // 충돌 없는 경로

            // when
            game.tick();

            // then
            assertThat(game.isRoundOver()).isTrue();
            final var rank = game.getResult().getRank();
            assertThat(rank.get(gamers.get(2))).isEqualTo(1);
            assertThat(rank.get(gamers.get(0))).isEqualTo(2);
            assertThat(rank.get(gamers.get(1))).isEqualTo(2);
        }
    }

    @Nested
    class 조향 {

        @Test
        void 회전은_각속도_상한으로_클램프된다() {
            // given — 목표각이 정반대(π)여도 한 틱에 ω만큼만 돈다.
            final WormGameRules rules = WormGameRules.defaults();
            final Worm worm = new Worm(GamerFixture.호스트_꾹이());
            worm.spawnAt(0, 0, 0);
            worm.steer(Math.PI, 1);

            // when
            worm.advance(rules.speedPerTick(1), rules.omegaPerTick(1));

            // then
            assertThat(worm.getAngle()).isCloseTo(rules.omegaPerTick(1), within(1e-9));
        }

        @Test
        void 회전_상한은_속도에_부분_비례한다() {
            // given
            final WormGameRules rules = WormGameRules.defaults();

            // when — 최고 속도(램프 완료) 시점의 ω / 초기 ω = 1.6^0.7
            final double ratio = rules.omegaPerTick(rules.speedRampTicks()) / rules.omegaPerTick(0);

            // then
            assertThat(ratio).isCloseTo(Math.pow(1.6, 0.7), within(1e-9));
        }

        @Test
        void 유효하지_않은_각도는_거부한다() {
            // given
            final WormGame game = new WormGame();
            game.setUp(twoGamers);
            game.updateState(WormGameState.PLAYING);

            // when & then
            assertCoffeeShoutException(() -> game.steer("꾹이", Double.NaN, 1), WormGameErrorCode.INVALID_STEERING);
        }

        @Test
        void 순서가_뒤바뀐_조향은_무시된다() {
            // given — 마지막 값만 유효: 늦게 도착한 과거 seq는 버린다.
            final WormGame game = new WormGame();
            game.setUp(twoGamers);
            game.updateState(WormGameState.PLAYING);
            final Worm worm = game.getWorms().findByName("꾹이");

            // when
            game.steer("꾹이", 1.0, 5);
            game.steer("꾹이", 2.0, 3);

            // then
            assertThat(worm.getTargetAngle()).isCloseTo(1.0, within(1e-9));
        }

        @Test
        void 플레이_중이_아니면_조향할_수_없다() {
            // given
            final WormGame game = new WormGame();
            game.setUp(twoGamers);

            // when & then
            assertCoffeeShoutException(() -> game.steer("꾹이", 1.0, 1), WormGameErrorCode.NOT_PLAYING_STATE);
        }
    }

    @Nested
    class 축소_곡선 {

        final WormGameRules rules = WormGameRules.defaults();

        @Test
        void 아레나_면적은_인원수에_비례한다() {
            assertThat(rules.initialRadius(4)).isCloseTo(220.0, within(1e-9));
            assertThat(rules.initialRadius(8)).isCloseTo(220.0 * Math.sqrt(2), within(1e-9));
        }

        @Test
        void 유예_동안은_줄지_않고_완료_시_최소_비율에_도달한다() {
            assertThat(rules.arenaRadius(4, 0)).isCloseTo(220.0, within(1e-9));
            assertThat(rules.arenaRadius(4, rules.shrinkDelayTicks() - 1)).isCloseTo(220.0, within(1e-9));
            assertThat(rules.arenaRadius(4, rules.shrinkDelayTicks() + rules.shrinkDurationTicks()))
                    .isCloseTo(220.0 * 0.30, within(1e-9));
        }

        @Test
        void 완료_후에도_완만히_줄다가_바닥에서_멈춘다() {
            final long doneTick = rules.shrinkDelayTicks() + rules.shrinkDurationTicks();
            assertThat(rules.arenaRadius(4, doneTick + 100)).isCloseTo(220.0 * 0.30 - 0.05 * 100, within(1e-9));
            assertThat(rules.arenaRadius(4, doneTick + 100_000)).isEqualTo(rules.minRadius());
        }
    }
}
