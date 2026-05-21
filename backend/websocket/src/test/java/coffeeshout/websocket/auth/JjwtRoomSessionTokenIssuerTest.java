package coffeeshout.websocket.auth;

import coffeeshout.room.infra.auth.RoomSessionClaim;
import coffeeshout.room.infra.auth.RoomSessionTokenService;
import coffeeshout.room.infra.auth.RoomSessionTokenIssuer;
import coffeeshout.room.infra.auth.JjwtRoomSessionTokenIssuer;
import coffeeshout.room.infra.auth.RoomSessionTokenProperties;
import coffeeshout.room.infra.auth.RoomSessionTokenErrorCode;

import coffeeshout.fixture.RoomSessionClaimFixture;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static coffeeshout.fixture.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class JjwtRoomSessionTokenIssuerTest {

    private static final String TEST_SECRET = "test-room-session-token-secret-key-must-be-at-least-256-bits-long";
    private static final RoomSessionTokenProperties TEST_PROPERTIES = new RoomSessionTokenProperties(TEST_SECRET);

    private JjwtRoomSessionTokenIssuer issuer;

    @BeforeEach
    void setUp() {
        issuer = new JjwtRoomSessionTokenIssuer(TEST_PROPERTIES, Duration.ofHours(1));
    }

    @Nested
    class 토큰_발급 {

        @Test
        void 발급된_토큰은_null이_아니다() {
            final String token = issuer.issue(RoomSessionClaimFixture.로그인_플레이어());

            assertThat(token).isNotBlank();
        }
    }

    @Nested
    class 토큰_검증_성공 {

        @Test
        void 로그인_플레이어_토큰을_발급하고_검증하면_동일한_클레임이_반환된다() {
            final RoomSessionClaim original = RoomSessionClaimFixture.로그인_플레이어();
            final String token = issuer.issue(original);

            final RoomSessionClaim verified = issuer.verify(token);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(verified.joinCode()).isEqualTo(original.joinCode());
                softly.assertThat(verified.playerName()).isEqualTo(original.playerName());
                softly.assertThat(verified.userId()).isEqualTo(original.userId());
            });
        }

        @Test
        void 익명_플레이어_토큰을_발급하고_검증하면_userId가_null이다() {
            final RoomSessionClaim original = RoomSessionClaimFixture.익명_플레이어();
            final String token = issuer.issue(original);

            final RoomSessionClaim verified = issuer.verify(token);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(verified.joinCode()).isEqualTo(original.joinCode());
                softly.assertThat(verified.playerName()).isEqualTo(original.playerName());
                softly.assertThat(verified.userId()).isNull();
            });
        }
    }

    @Nested
    class 토큰_검증_실패 {

        @Test
        void 만료된_토큰_검증_시_ROOM_TOKEN_EXPIRED_예외가_발생한다() {
            final JjwtRoomSessionTokenIssuer shortLivedIssuer =
                    new JjwtRoomSessionTokenIssuer(TEST_PROPERTIES, Duration.ofMillis(1));
            final String token = shortLivedIssuer.issue(RoomSessionClaimFixture.로그인_플레이어());

            await().atMost(Duration.ofSeconds(2))
                    .untilAsserted(() -> assertCoffeeShoutException(
                            () -> shortLivedIssuer.verify(token),
                            RoomSessionTokenErrorCode.ROOM_TOKEN_EXPIRED));
        }

        @Test
        void 위조된_토큰_검증_시_ROOM_TOKEN_INVALID_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> issuer.verify("invalid.token.value"),
                    RoomSessionTokenErrorCode.ROOM_TOKEN_INVALID);
        }

        @Test
        void 다른_키로_서명된_토큰_검증_시_ROOM_TOKEN_INVALID_예외가_발생한다() {
            final JjwtRoomSessionTokenIssuer otherIssuer = new JjwtRoomSessionTokenIssuer(
                    new RoomSessionTokenProperties("other-secret-key-must-be-at-least-256-bits-long-here"),
                    Duration.ofHours(1));
            final String tokenFromOtherKey = otherIssuer.issue(RoomSessionClaimFixture.로그인_플레이어());

            assertCoffeeShoutException(
                    () -> issuer.verify(tokenFromOtherKey),
                    RoomSessionTokenErrorCode.ROOM_TOKEN_INVALID);
        }
    }
}
