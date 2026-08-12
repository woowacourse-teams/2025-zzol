package coffeeshout.websocket.config;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.WebsocketModuleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.broker.OrderedMessageChannelDecorator;

class WebSocketMessageBrokerConfigTest extends WebsocketModuleIntegrationTest {

    @Autowired
    @Qualifier("clientOutboundChannel")
    MessageChannel clientOutboundChannel;

    /**
     * clientOutboundChannel은 16스레드 풀이라(WebSocketMessageBrokerConfig#configureClientOutboundChannel)
     * 순서 보존 설정이 없으면 같은 세션으로 가는 브로드캐스트가 뒤바뀐 순서로 도착할 수 있다.
     *
     * <p>실제 역전은 스레드 스케줄링에 달려 있어 재현이 확률적이다. 그래서 "역전이 일어나는가"를 재는 대신
     * <b>순서를 보존하는 설정이 실제로 적용됐는가</b>를 잰다. {@code setPreservePublishOrder(true)}는
     * clientOutboundChannel에 {@link OrderedMessageChannelDecorator}의 인터셉터를 설치하며,
     * {@code supportsOrderedMessages}가 그 설치 여부를 그대로 관측한다 — 부하와 무관하게 결정론적이다.
     */
    @Test
    void 클라이언트_아웃바운드_채널은_발행_순서를_보존한다() {
        assertThat(OrderedMessageChannelDecorator.supportsOrderedMessages(clientOutboundChannel)).isTrue();
    }
}
