package coffeeshout.room.domain.player;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerTest {

    @Nested
    class 호스트_생성 {

        @Test
        void 익명_호스트는_userId가_null이다() {
            final Player host = Player.createHost(new PlayerName("꾹이"));

            assertThat(host.getUserId()).isNull();
        }

        @Test
        void 회원_호스트는_userId가_저장된다() {
            final Player host = Player.createHost(new PlayerName("꾹이"), 42L);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(host.getUserId()).isEqualTo(42L);
                softly.assertThat(host.getName().value()).isEqualTo("꾹이");
                softly.assertThat(host.getPlayerType()).isEqualTo(PlayerType.HOST);
            });
        }
    }

    @Nested
    class 게스트_생성 {

        @Test
        void 익명_게스트는_userId가_null이다() {
            final Player guest = Player.createGuest(new PlayerName("루키"));

            assertThat(guest.getUserId()).isNull();
        }

        @Test
        void 회원_게스트는_userId가_저장된다() {
            final Player guest = Player.createGuest(new PlayerName("루키"), 99L);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(guest.getUserId()).isEqualTo(99L);
                softly.assertThat(guest.getName().value()).isEqualTo("루키");
                softly.assertThat(guest.getPlayerType()).isEqualTo(PlayerType.GUEST);
            });
        }
    }
}
