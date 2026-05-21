package coffeeshout.laddergame.domain;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.minigame.domain.Gamer;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PolesTest {

    Gamer 꾹이;
    Gamer 루키;
    Gamer 엠제이;
    List<Gamer> gamers;
    Poles poles;

    @BeforeEach
    void setUp() {
        꾹이 = Gamer.guest(new PlayerName("꾹이"));
        루키 = Gamer.guest(new PlayerName("루키"));
        엠제이 = Gamer.guest(new PlayerName("엠제이"));
        gamers = List.of(꾹이, 루키, 엠제이);
        poles = Poles.assign(gamers);
    }

    @Nested
    class assign_테스트 {

        @Test
        void 모든_플레이어가_기둥에_배정된다() {
            assertThat(poles.getAll()).hasSize(3);
        }

        @Test
        void 기둥_인덱스는_0부터_n_minus_1까지다() {
            final List<Integer> indices = poles.getAll().stream()
                    .map(Pole::index)
                    .sorted()
                    .toList();

            assertThat(indices).containsExactly(0, 1, 2);
        }

        @Test
        void 각_기둥에_플레이어가_하나씩_배정된다() {
            final long distinctPlayers = poles.getAll().stream()
                    .map(p -> p.gamer().name().value())
                    .distinct()
                    .count();

            assertThat(distinctPlayers).isEqualTo(3);
        }
    }

    @Nested
    class getPoleIndex_테스트 {

        @Test
        void 등록된_플레이어의_기둥_인덱스를_반환한다() {
            final int index = poles.getPoleIndex(꾹이);

            assertThat(index).isBetween(0, 2);
        }

        @Test
        void 미등록_플레이어_조회_시_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> poles.getPoleIndex(Gamer.guest(new PlayerName("없는플레이어"))),
                    LadderGameErrorCode.PLAYER_NOT_FOUND
            );
        }
    }

    @Nested
    class getGamer_테스트 {

        @Test
        void 기둥_인덱스로_Gamer를_가져온다() {
            final int index = poles.getPoleIndex(꾹이);
            final Gamer found = poles.getGamer(index);

            assertThat(found.name().value()).isEqualTo("꾹이");
        }

        @Test
        void 유효하지_않은_인덱스_조회_시_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> poles.getGamer(99),
                    LadderGameErrorCode.INVALID_POLE_INDEX
            );
        }
    }

    @Nested
    class isValidSegment_테스트 {

        @Test
        void 기둥_수_minus_2까지_유효하다() {
            // 기둥 3개: 유효한 구간 인덱스 = 0, 1
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(poles.isValidSegment(0)).isTrue();
                softly.assertThat(poles.isValidSegment(1)).isTrue();
            });
        }

        @Test
        void 기둥_수_minus_1은_유효하지_않다() {
            assertThat(poles.isValidSegment(2)).isFalse();
        }

        @Test
        void 음수_구간_인덱스는_유효하지_않다() {
            assertThat(poles.isValidSegment(-1)).isFalse();
        }
    }

    @Nested
    class contains_테스트 {

        @Test
        void 등록된_플레이어는_true를_반환한다() {
            assertThat(poles.contains(꾹이)).isTrue();
        }

        @Test
        void 미등록_플레이어는_false를_반환한다() {
            assertThat(poles.contains(Gamer.guest(new PlayerName("없는플레이어")))).isFalse();
        }
    }

    @Nested
    class size_테스트 {

        @Test
        void 플레이어_수와_동일한_크기를_반환한다() {
            assertThat(poles.size()).isEqualTo(3);
        }
    }
}
