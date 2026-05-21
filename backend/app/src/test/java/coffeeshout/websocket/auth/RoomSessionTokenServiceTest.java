package coffeeshout.websocket.auth;

import coffeeshout.room.infra.auth.RoomSessionClaim;
import coffeeshout.room.infra.auth.RoomSessionTokenService;
import coffeeshout.room.infra.auth.RoomSessionTokenIssuer;
import coffeeshout.room.infra.auth.JjwtRoomSessionTokenIssuer;
import coffeeshout.room.infra.auth.RoomSessionTokenProperties;
import coffeeshout.room.infra.auth.RoomSessionTokenErrorCode;

import coffeeshout.fixture.RoomSessionClaimFixture;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomSessionTokenServiceTest {

    @Mock
    private RoomSessionTokenIssuer issuer;

    @InjectMocks
    private RoomSessionTokenService service;

    @Nested
    class 토큰_발급 {

        @Test
        void 발급_요청을_Issuer에_위임하고_토큰을_반환한다() {
            given(issuer.issue(any())).willReturn("mocked-token");

            final String token = service.issue("ABCD", "홍길동", 42L);

            assertThat(token).isEqualTo("mocked-token");
            verify(issuer).issue(RoomSessionClaim.of("ABCD", "홍길동", 42L));
        }

        @Test
        void 익명_플레이어_발급_시_userId_null로_Issuer에_위임한다() {
            given(issuer.issue(any())).willReturn("anon-token");

            service.issue("ABCD", "익명이", null);

            verify(issuer).issue(RoomSessionClaim.ofAnonymous("ABCD", "익명이"));
        }
    }

    @Nested
    class 토큰_검증 {

        @Test
        void 검증_요청을_Issuer에_위임하고_클레임을_반환한다() {
            final RoomSessionClaim expected = RoomSessionClaimFixture.로그인_플레이어();
            given(issuer.verify("some-token")).willReturn(expected);

            final RoomSessionClaim result = service.verify("some-token");

            assertThat(result).isEqualTo(expected);
            verify(issuer).verify("some-token");
        }
    }
}
