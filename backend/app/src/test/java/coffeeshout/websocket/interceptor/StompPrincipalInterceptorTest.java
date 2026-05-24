package coffeeshout.websocket.interceptor;

import coffeeshout.support.TestStompSession;
import coffeeshout.fixture.UserFixture;
import coffeeshout.fixture.RoomWebSocketTestSupport;
import coffeeshout.user.application.service.AuthTokenService;
import coffeeshout.user.domain.TokenPair;
import coffeeshout.user.domain.User;
import coffeeshout.user.domain.repository.UserRepository;
import coffeeshout.websocket.UserPrincipal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.ExecutionException;
import org.springframework.messaging.simp.stomp.ConnectionLostException;

class StompPrincipalInterceptorTest extends RoomWebSocketTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Nested
    class 유효한_roomToken_헤더 {

        @Test
        void 정상_토큰으로_연결하면_성공한다() throws Exception {
            final TestStompSession session = createSession("ABCD", "홍길동");

            assertThat(session.isConnected()).isTrue();
            assertThat(session.getPrincipalName()).isEqualTo("ABCD:홍길동");

            session.disconnect();
        }
    }

    @Nested
    class 유효하지_않은_roomToken_헤더 {

        @Test
        void 위조된_토큰으로_연결하면_거부된다() {
            assertThatThrownBy(() -> createSessionWithRoomToken("invalid.token.value"))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ConnectionLostException.class);
        }
    }

    @Nested
    class Authorization_Bearer_헤더 {

        @Test
        void 유효한_액세스_토큰으로_연결하면_user_principal로_성공한다() throws Exception {
            final User user = userRepository.save(UserFixture.회원_엠제이());
            final TokenPair tokens = authTokenService.issue(user);

            final TestStompSession session = createSessionWithAuthorizationToken(tokens.accessToken());

            assertThat(session.isConnected()).isTrue();
            assertThat(session.getPrincipalName()).isEqualTo(UserPrincipal.of(user.getId()));

            session.disconnect();
        }

        @Test
        void 유효하지_않은_액세스_토큰이면_sessionId로_연결된다() throws Exception {
            final TestStompSession session = createSessionWithAuthorizationToken("invalid.token");

            assertThat(session.isConnected()).isTrue();
            assertThat(session.getPrincipalName()).isNotNull();

            session.disconnect();
        }

        @Test
        void Authorization_헤더_없으면_sessionId로_연결된다() throws Exception {
            final TestStompSession session = createSessionWithoutRoomToken();

            assertThat(session.isConnected()).isTrue();

            session.disconnect();
        }
    }
}
