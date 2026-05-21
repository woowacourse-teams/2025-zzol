package coffeeshout.minigame.domain;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.room.domain.player.PlayerName;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GamerTest {

    @Nested
    class 로그인_여부_판단 {

        @Test
        void userId가_있으면_로그인_사용자이다() {
            final Gamer gamer = new Gamer(new PlayerName("한스"), 100L);

            assertThat(gamer.isLoggedIn()).isTrue();
        }

        @Test
        void userId가_null이면_비로그인_사용자이다() {
            final Gamer gamer = new Gamer(new PlayerName("한스"), null);

            assertThat(gamer.isLoggedIn()).isFalse();
        }
    }

    @Nested
    class 필드_접근 {

        @Test
        void name과_userId를_올바르게_반환한다() {
            final PlayerName name = new PlayerName("꾹이");
            final Gamer gamer = new Gamer(name, 200L);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(gamer.name()).isEqualTo(name);
                softly.assertThat(gamer.userId()).isEqualTo(200L);
            });
        }
    }
}
