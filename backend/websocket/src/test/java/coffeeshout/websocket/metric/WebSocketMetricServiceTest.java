package coffeeshout.websocket.metric;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WebSocketMetricServiceTest {

    private static final String GAUGE_NAME = "websocket.connections.current";
    private static final String DISCONNECT_REASON = "CLIENT_DISCONNECT";

    SimpleMeterRegistry meterRegistry;
    WebSocketMetricService metricService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricService = new WebSocketMetricService(meterRegistry);
        metricService.initializeMetrics();
    }

    private double currentConnections() {
        return meterRegistry.get(GAUGE_NAME).gauge().value();
    }

    @Nested
    class 현재_연결_수_게이지 {

        @Test
        void 연결_수립_후_해제하면_원래_값으로_돌아온다() {
            metricService.startConnection("session-1");
            metricService.completeConnection("session-1");
            assertThat(currentConnections()).isEqualTo(1);

            metricService.recordDisconnection("session-1", DISCONNECT_REASON);

            assertThat(currentConnections()).isZero();
        }

        @Test
        void 연결을_수립하지_않은_세션이_해제돼도_값이_내려가지_않는다() {
            metricService.startConnection("session-1");
            metricService.completeConnection("session-1");

            metricService.recordDisconnection("probe-session", DISCONNECT_REASON);

            assertThat(currentConnections()).isEqualTo(1);
        }

        @Test
        void 연결_수립_여부로_프로브_세션을_구분한다() {
            metricService.startConnection("probe-session");
            metricService.completeConnection("session-1");

            assertThat(metricService.hasEstablishedConnection("session-1")).isTrue();
            assertThat(metricService.hasEstablishedConnection("probe-session")).isFalse();
        }

        @Test
        void 같은_세션이_두_번_해제돼도_값이_음수가_되지_않는다() {
            metricService.completeConnection("session-1");

            metricService.recordDisconnection("session-1", DISCONNECT_REASON);
            metricService.recordDisconnection("session-1", DISCONNECT_REASON);

            assertThat(currentConnections()).isZero();
        }
    }
}
