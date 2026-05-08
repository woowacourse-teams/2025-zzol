package coffeeshout.global.websocket.auth;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomSessionClaimTest {

    @Nested
    class 생성_성공 {

        @Test
        void 로그인_사용자_클레임을_생성한다() {
            final RoomSessionClaim claim = RoomSessionClaim.of("ABCD", "홍길동", 42L);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(claim.joinCode()).isEqualTo("ABCD");
                softly.assertThat(claim.playerName()).isEqualTo("홍길동");
                softly.assertThat(claim.userId()).isEqualTo(42L);
            });
        }

        @Test
        void 익명_플레이어_클레임을_생성한다() {
            final RoomSessionClaim claim = RoomSessionClaim.ofAnonymous("ABCD", "홍길동");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(claim.joinCode()).isEqualTo("ABCD");
                softly.assertThat(claim.playerName()).isEqualTo("홍길동");
                softly.assertThat(claim.userId()).isNull();
            });
        }

        @Test
        void userId가_null이어도_생성된다() {
            final RoomSessionClaim claim = RoomSessionClaim.of("ABCD", "홍길동", null);

            assertThat(claim.userId()).isNull();
        }
    }

    @Nested
    class 생성_실패 {

        @Test
        void joinCode가_null이면_예외가_발생한다() {
            assertThatThrownBy(() -> RoomSessionClaim.of(null, "홍길동", 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("joinCode");
        }

        @Test
        void joinCode가_빈_문자열이면_예외가_발생한다() {
            assertThatThrownBy(() -> RoomSessionClaim.of("   ", "홍길동", 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("joinCode");
        }

        @Test
        void playerName이_null이면_예외가_발생한다() {
            assertThatThrownBy(() -> RoomSessionClaim.of("ABCD", null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("playerName");
        }

        @Test
        void playerName이_빈_문자열이면_예외가_발생한다() {
            assertThatThrownBy(() -> RoomSessionClaim.of("ABCD", "   ", 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("playerName");
        }
    }
}
