package coffeeshout.laddergame.domain;

import static coffeeshout.fixture.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.PlayerFixture;
import coffeeshout.laddergame.domain.LadderGameErrorCode;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PolesTest {

    Player 꾹이;
    Player 루키;
    Player 엠제이;
    List<Player> players;
    Poles poles;

    @BeforeEach
    void setUp() {
        꾹이 = PlayerFixture.호스트꾹이();
        루키 = PlayerFixture.게스트루키();
        엠제이 = PlayerFixture.게스트엠제이();
        players = List.of(꾹이, 루키, 엠제이);
        poles = Poles.assign(players);
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
                    .map(p -> p.player().getName().value())
                    .distinct()
                    .count();

            assertThat(distinctPlayers).isEqualTo(3);
        }
    }

    @Nested
    class getPoleIndex_테스트 {

        @Test
        void 등록된_플레이어의_기둥_인덱스를_반환한다() {
            final int index = poles.getPoleIndex(new PlayerName("꾹이"));

            assertThat(index).isBetween(0, 2);
        }

        @Test
        void 미등록_플레이어_조회_시_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> poles.getPoleIndex(new PlayerName("없는플레이어")),
                    LadderGameErrorCode.PLAYER_NOT_FOUND
            );
        }
    }

    @Nested
    class getPlayer_테스트 {

        @Test
        void 기둥_인덱스로_플레이어를_가져온다() {
            final int index = poles.getPoleIndex(new PlayerName("꾹이"));
            final Player found = poles.getPlayer(index);

            assertThat(found.getName().value()).isEqualTo("꾹이");
        }

        @Test
        void 유효하지_않은_인덱스_조회_시_예외를_던진다() {
            assertCoffeeShoutException(
                    () -> poles.getPlayer(99),
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
            assertThat(poles.contains(new PlayerName("꾹이"))).isTrue();
        }

        @Test
        void 미등록_플레이어는_false를_반환한다() {
            assertThat(poles.contains(new PlayerName("없는플레이어"))).isFalse();
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
