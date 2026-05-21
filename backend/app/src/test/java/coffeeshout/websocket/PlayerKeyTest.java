package coffeeshout.websocket;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerKeyTest {

    @Nested
    class userId_포함_생성 {

        @Test
        void userId가_있으면_세_파트_문자열로_직렬화된다() {
            PlayerKey key = PlayerKey.of("ABC23", "player1", 42L);

            assertThat(key.toString()).isEqualTo("ABC23:player1:42");
        }

        @Test
        void userId가_null이면_두_파트_문자열로_직렬화된다() {
            PlayerKey key = PlayerKey.of("ABC23", "player1", null);

            assertThat(key.toString()).isEqualTo("ABC23:player1");
        }

        @Test
        void userId_없는_팩토리는_userId를_null로_설정한다() {
            PlayerKey key = PlayerKey.of("ABC23", "player1");

            assertThat(key.userId()).isNull();
        }

        @Test
        void userId_있는_키의_모든_필드가_올바르게_설정된다() {
            PlayerKey key = PlayerKey.of("ABC23", "player1", 99L);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(key.joinCode()).isEqualTo("ABC23");
                softly.assertThat(key.playerName()).isEqualTo("player1");
                softly.assertThat(key.userId()).isEqualTo(99L);
            });
        }
    }

    @Nested
    class userId_포함_파싱 {

        @Test
        void userId_포함_세_파트_문자열을_파싱한다() {
            PlayerKey key = PlayerKey.parse("ABC23:player1:42");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(key.joinCode()).isEqualTo("ABC23");
                softly.assertThat(key.playerName()).isEqualTo("player1");
                softly.assertThat(key.userId()).isEqualTo(42L);
            });
        }

        @Test
        void 두_파트_문자열_파싱_시_userId는_null이다() {
            PlayerKey key = PlayerKey.parse("ABC23:player1");

            assertThat(key.userId()).isNull();
        }

        @Test
        void 세_번째_파트가_숫자가_아니면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> PlayerKey.parse("ABC23:player1:extra"),
                    PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT
            );
        }

        @Test
        void 세_번째_파트가_비어있으면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> PlayerKey.parse("ABC23:player1:"),
                    PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT
            );
        }

        @Test
        void 네_파트_이상이면_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> PlayerKey.parse("ABC23:player1:42:extra"),
                    PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT
            );
        }

        @Test
        void null_입력이면_NullPointerException이_발생한다() {
            assertThatThrownBy(() -> PlayerKey.parse(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class userId_포함_유효성_검증 {

        @Test
        void userId가_있는_세_파트_문자열은_유효하다() {
            assertThat(PlayerKey.isValid("ABC23:player1:42")).isTrue();
        }

        @Test
        void 세_번째_파트가_숫자가_아니면_유효하지_않다() {
            assertThat(PlayerKey.isValid("ABC23:player1:abc")).isFalse();
        }

        @Test
        void 세_번째_파트가_비어있으면_유효하지_않다() {
            assertThat(PlayerKey.isValid("ABC23:player1:")).isFalse();
        }

        @Test
        void 네_파트_이상이면_유효하지_않다() {
            assertThat(PlayerKey.isValid("ABC23:player1:42:extra")).isFalse();
        }
    }

    @Nested
    class 직렬화_역직렬화_대칭성 {

        @Test
        void userId_있는_키는_직렬화_후_파싱해도_동일하다() {
            PlayerKey original = PlayerKey.of("ABC23", "player1", 42L);
            PlayerKey parsed = PlayerKey.parse(original.toString());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(parsed.joinCode()).isEqualTo(original.joinCode());
                softly.assertThat(parsed.playerName()).isEqualTo(original.playerName());
                softly.assertThat(parsed.userId()).isEqualTo(original.userId());
            });
        }

        @Test
        void userId_없는_키는_직렬화_후_파싱해도_동일하다() {
            PlayerKey original = PlayerKey.of("ABC23", "player1");
            PlayerKey parsed = PlayerKey.parse(original.toString());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(parsed.joinCode()).isEqualTo(original.joinCode());
                softly.assertThat(parsed.playerName()).isEqualTo(original.playerName());
                softly.assertThat(parsed.userId()).isNull();
            });
        }
    }
}
