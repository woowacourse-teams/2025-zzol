package coffeeshout.room.domain.player;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerTest {

    @Nested
    class 호스트_생성 {

        @Test
        void 익명_호스트는_userId와_userCode가_null이다() {
            final Player host = Player.createHost(new PlayerName("꾹이"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(host.getUserId()).isNull();
                softly.assertThat(host.getUserCode()).isNull();
            });
        }

        @Test
        void 회원_호스트는_userId와_userCode가_저장된다() {
            final Player host = Player.createHost(new PlayerName("꾹이"), 42L, "ABCD1");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(host.getUserId()).isEqualTo(42L);
                softly.assertThat(host.getUserCode()).isEqualTo("ABCD1");
                softly.assertThat(host.getName().value()).isEqualTo("꾹이");
                softly.assertThat(host.getPlayerType()).isEqualTo(PlayerType.HOST);
            });
        }

        @Test
        void userId만_있는_호스트는_userCode가_null이다() {
            final Player host = Player.createHost(new PlayerName("꾹이"), 42L);

            assertThat(host.getUserCode()).isNull();
        }
    }

    @Nested
    class 게스트_생성 {

        @Test
        void 익명_게스트는_userId와_userCode가_null이다() {
            final Player guest = Player.createGuest(new PlayerName("루키"));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(guest.getUserId()).isNull();
                softly.assertThat(guest.getUserCode()).isNull();
            });
        }

        @Test
        void 회원_게스트는_userId와_userCode가_저장된다() {
            final Player guest = Player.createGuest(new PlayerName("루키"), 99L, "XYZ99");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(guest.getUserId()).isEqualTo(99L);
                softly.assertThat(guest.getUserCode()).isEqualTo("XYZ99");
                softly.assertThat(guest.getName().value()).isEqualTo("루키");
                softly.assertThat(guest.getPlayerType()).isEqualTo(PlayerType.GUEST);
            });
        }

        @Test
        void userId만_있는_게스트는_userCode가_null이다() {
            final Player guest = Player.createGuest(new PlayerName("루키"), 99L);

            assertThat(guest.getUserCode()).isNull();
        }
    }
}
