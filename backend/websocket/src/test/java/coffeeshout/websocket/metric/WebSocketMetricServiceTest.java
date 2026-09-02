package coffeeshout.websocket.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WebSocketMetricServiceTest {

    private static final String GAUGE_NAME = "websocket.connections.current";
    private static final String DISCONNECT_COUNTER_NAME = "websocket.connections.disconnected";
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

    private double disconnectedCount() {
        return meterRegistry.find(DISCONNECT_COUNTER_NAME).tag("reason", DISCONNECT_REASON).counters().stream()
                .mapToDouble(counter -> counter.count())
                .sum();
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
    }

    @Nested
    class 해제_건수_카운터 {

        @Test
        void 같은_세션에_해제_이벤트가_두_번_와도_한_번만_센다() {
            metricService.completeConnection("session-1");

            metricService.recordDisconnection("session-1", DISCONNECT_REASON);
            metricService.recordDisconnection("session-1", DISCONNECT_REASON);

            assertSoftly(softly -> {
                softly.assertThat(disconnectedCount()).isEqualTo(1);
                softly.assertThat(currentConnections()).isZero();
            });
        }

        @Test
        void 연결을_수립하지_않은_세션의_해제는_세지_않는다() {
            metricService.startConnection("probe-session");

            metricService.recordDisconnection("probe-session", DISCONNECT_REASON);

            assertThat(disconnectedCount()).isZero();
        }
    }

    @Nested
    class 첫_해제_여부_반환 {

        @Test
        void 연결을_수립한_세션의_첫_해제만_참을_받는다() {
            metricService.completeConnection("session-1");

            assertSoftly(softly -> {
                softly.assertThat(metricService.recordDisconnection("session-1", DISCONNECT_REASON))
                        .isTrue();
                softly.assertThat(metricService.recordDisconnection("session-1", DISCONNECT_REASON))
                        .isFalse();
            });
        }

        @Test
        void 연결을_수립하지_않은_세션의_해제는_거짓을_받는다() {
            metricService.startConnection("probe-session");

            assertThat(metricService.recordDisconnection("probe-session", DISCONNECT_REASON))
                    .isFalse();
        }
    }
}
