package coffeeshout.minigame.domain;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coffeeshout.exception.custom.BusinessException;
import coffeeshout.room.domain.player.PlayerName;
import java.util.List;
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

    @Nested
    class 팩토리_메서드 {

        @Test
        void guest_생성시_userId가_null이다() {
            final Gamer gamer = Gamer.guest(new PlayerName("한스"));

            assertThat(gamer.userId()).isNull();
        }

        @Test
        void loggedIn_생성시_userId가_설정된다() {
            final Gamer gamer = Gamer.loggedIn(new PlayerName("한스"), 100L);

            assertThat(gamer.userId()).isEqualTo(100L);
        }

        @Test
        void name이_null이면_NPE가_발생한다() {
            assertThatThrownBy(() -> new Gamer(null, 100L))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class userId_조회 {

        @Test
        void 비로그인_사용자는_Optional_empty를_반환한다() {
            final Gamer gamer = Gamer.guest(new PlayerName("한스"));

            assertThat(gamer.userIdOptional()).isEmpty();
        }

        @Test
        void 로그인_사용자는_userId를_담은_Optional을_반환한다() {
            final Gamer gamer = Gamer.loggedIn(new PlayerName("한스"), 100L);

            assertThat(gamer.userIdOptional()).hasValue(100L);
        }
    }

    @Nested
    class 소유권_검증 {

        @Test
        void 로그인_사용자는_이름과_userId가_모두_일치하면_통과한다() {
            final Gamer registered = Gamer.loggedIn(new PlayerName("한스"), 100L);
            final Gamer requester = Gamer.loggedIn(new PlayerName("한스"), 100L);

            assertThatNoException().isThrownBy(() -> requester.validateAgainst(List.of(registered)));
        }

        @Test
        void 비로그인_사용자는_이름만_일치하면_통과한다() {
            final Gamer registered = Gamer.guest(new PlayerName("한스"));
            final Gamer requester = Gamer.guest(new PlayerName("한스"));

            assertThatNoException().isThrownBy(() -> requester.validateAgainst(List.of(registered)));
        }

        @Test
        void 로그인_사용자가_다른_userId로_요청하면_예외를_던진다() {
            final Gamer registered = Gamer.loggedIn(new PlayerName("한스"), 100L);
            final Gamer requester = Gamer.loggedIn(new PlayerName("한스"), 999L);

            assertCoffeeShoutException(
                    () -> requester.validateAgainst(List.of(registered)),
                    GamerErrorCode.UNAUTHORIZED_GAMER
            );
        }

        @Test
        void 로그인된_게이머를_비로그인으로_요청하면_예외를_던진다() {
            final Gamer registered = Gamer.loggedIn(new PlayerName("한스"), 100L);
            final Gamer requester = Gamer.guest(new PlayerName("한스"));

            assertCoffeeShoutException(
                    () -> requester.validateAgainst(List.of(registered)),
                    GamerErrorCode.UNAUTHORIZED_GAMER
            );
        }

        @Test
        void 등록되지_않은_이름으로_요청하면_예외를_던진다() {
            final Gamer registered = Gamer.guest(new PlayerName("한스"));
            final Gamer requester = Gamer.guest(new PlayerName("유령"));

            assertCoffeeShoutException(
                    () -> requester.validateAgainst(List.of(registered)),
                    GamerErrorCode.UNAUTHORIZED_GAMER
            );
        }
    }
}
