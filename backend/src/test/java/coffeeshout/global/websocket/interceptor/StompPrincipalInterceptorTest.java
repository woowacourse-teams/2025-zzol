package coffeeshout.global.websocket.interceptor;

import coffeeshout.fixture.TestStompSession;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.ExecutionException;
import org.springframework.messaging.simp.stomp.ConnectionLostException;

class StompPrincipalInterceptorTest extends WebSocketIntegrationTestSupport {

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
        void roomToken_헤더가_없으면_연결이_거부된다() {
            assertThatThrownBy(() -> createSessionWithoutRoomToken())
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ConnectionLostException.class);
        }

        @Test
        void 위조된_토큰으로_연결하면_거부된다() {
            assertThatThrownBy(() -> createSessionWithRoomToken("invalid.token.value"))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ConnectionLostException.class);
        }
    }
}
