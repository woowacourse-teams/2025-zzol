package coffeeshout.user.ui.resolver;

import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import coffeeshout.fixture.RoomSessionClaimFixture;
import coffeeshout.websocket.auth.RoomSessionClaim;
import coffeeshout.websocket.auth.RoomSessionTokenErrorCode;
import coffeeshout.websocket.auth.RoomSessionTokenService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

@ExtendWith(MockitoExtension.class)
class RoomSessionClaimArgumentResolverTest {

    @Mock
    private RoomSessionTokenService roomSessionTokenService;

    @InjectMocks
    private RoomSessionClaimArgumentResolver resolver;

    @Nested
    class 파라미터_지원_여부 {

        @Test
        @SuppressWarnings("unchecked")
        void RoomSession_어노테이션과_RoomSessionClaim_타입이면_true를_반환한다() {
            MethodParameter parameter = mock(MethodParameter.class);
            given(parameter.hasParameterAnnotation(RoomSession.class)).willReturn(true);
            given(parameter.getParameterType()).willReturn((Class) RoomSessionClaim.class);

            assertThat(resolver.supportsParameter(parameter)).isTrue();
        }

        @Test
        void RoomSession_어노테이션이_없으면_false를_반환한다() {
            MethodParameter parameter = mock(MethodParameter.class);
            given(parameter.hasParameterAnnotation(RoomSession.class)).willReturn(false);

            assertThat(resolver.supportsParameter(parameter)).isFalse();
        }

        @Test
        @SuppressWarnings("unchecked")
        void RoomSession_어노테이션이_있어도_타입이_RoomSessionClaim이_아니면_false를_반환한다() {
            MethodParameter parameter = mock(MethodParameter.class);
            given(parameter.hasParameterAnnotation(RoomSession.class)).willReturn(true);
            given(parameter.getParameterType()).willReturn((Class) String.class);

            assertThat(resolver.supportsParameter(parameter)).isFalse();
        }
    }

    @Nested
    class 클레임_추출 {

        @Test
        void roomToken_헤더가_있으면_검증_후_클레임을_반환한다() {
            MockHttpServletRequest httpRequest = new MockHttpServletRequest();
            httpRequest.addHeader("roomToken", "valid-token");
            RoomSessionClaim expected = RoomSessionClaimFixture.로그인_플레이어();
            given(roomSessionTokenService.verify("valid-token")).willReturn(expected);

            Object result = resolver.resolveArgument(
                    mock(MethodParameter.class), null, new ServletWebRequest(httpRequest), null
            );

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void roomToken_헤더가_없으면_ROOM_TOKEN_MISSING_예외가_발생한다() {
            MockHttpServletRequest httpRequest = new MockHttpServletRequest();

            assertCoffeeShoutException(
                    () -> resolver.resolveArgument(
                            mock(MethodParameter.class), null, new ServletWebRequest(httpRequest), null
                    ),
                    RoomSessionTokenErrorCode.ROOM_TOKEN_MISSING
            );
        }

        @Test
        void roomToken_헤더가_공백이면_ROOM_TOKEN_MISSING_예외가_발생한다() {
            MockHttpServletRequest httpRequest = new MockHttpServletRequest();
            httpRequest.addHeader("roomToken", "   ");

            assertCoffeeShoutException(
                    () -> resolver.resolveArgument(
                            mock(MethodParameter.class), null, new ServletWebRequest(httpRequest), null
                    ),
                    RoomSessionTokenErrorCode.ROOM_TOKEN_MISSING
            );
        }
    }
}
