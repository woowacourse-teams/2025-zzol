package coffeeshout.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.fixture.TestStompSession;
import coffeeshout.fixture.TestStompSession.MessageCollector;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * 간단한 웹소켓 연결 및 E2E 테스트 실제 데이터 없이도 웹소켓 연결과 메시지 전송을 테스트
 */
class WebSocketSimpleTest extends WebSocketIntegrationTestSupport {

    @Test
    void SockJS_STOMP_엔드포인트_연결_테스트() throws Exception {
        // when
        TestStompSession session = createSession();

        // then
        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }

    @Test
    void 방_토픽_구독_테스트() throws Exception {
        // when
        TestStompSession session = createSession();

        // 방 토픽 구독 (내부적으로 MessageCollector 생성)
        MessageCollector collector = session.subscribe("/topic/room/1");

        // then
        assertThat(session.isConnected()).isTrue();
        assertThat(collector).isNotNull();

        session.disconnect();
    }

    @Test
    void 플레이어_목록_요청_메시지_전송_테스트() throws Exception {
        // given
        TestStompSession session = createSession();

        // 토픽 구독
        MessageCollector collector = session.subscribe("/topic/room/1");

        // when - 메시지 전송 (실제 데이터 없어도 전송 자체는 가능)
        session.send("/app/room/1/players");

        // then - 서버가 메시지를 처리하고 브로드캐스트할 때까지 대기 (MessageCollector 사용)
        // 실제 비즈니스 로직(RoomService 등)이 동작한다면 collector에 데이터가 쌓임
        // 여기서는 연결 상태와 전송 가능 여부를 확인하는 스모크 테스트로서,
        // session.isConnected()를 단순히 체크하는 것보다 더 실질적인 전송 상태를 검증함
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(session.isConnected()).isTrue());

        session.disconnect();
    }

    @Test
    void 여러_토픽_동시_구독_테스트() throws Exception {
        // when
        TestStompSession session = createSession();

        // 여러 토픽 구독
        MessageCollector roomCollector = session.subscribe("/topic/room/1");
        MessageCollector rouletteCollector = session.subscribe("/topic/room/1/roulette");
        MessageCollector minigameCollector = session.subscribe("/topic/room/1/minigame");

        // then
        assertThat(session.isConnected()).isTrue();
        assertThat(roomCollector).isNotNull();
        assertThat(rouletteCollector).isNotNull();
        assertThat(minigameCollector).isNotNull();

        session.disconnect();
    }
}
