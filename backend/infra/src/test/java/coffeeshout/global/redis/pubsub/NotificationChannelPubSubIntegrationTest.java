package coffeeshout.global.redis.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.InfraModuleIntegrationTest;
import coffeeshout.global.notify.GameNotificationChannel;
import coffeeshout.global.notify.NotificationSink;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 실제 Redis(Valkey 컨테이너)로 발행→수신 왕복과 traceparent 전파를 확인한다.
 * <p>
 * sink는 fake를 컨텍스트에 넣어 확인한다 — 실제 전달 구현({@code WsNotificationSink})은 {@code :websocket}에
 * 있어 인프라 컨텍스트에 없고, 이 테스트가 보려는 것도 STOMP 전달이 아니라 채널 경계의 왕복이다.
 */
@Import(NotificationChannelPubSubIntegrationTest.NotificationSinkTestConfig.class)
@DisplayName("알림 채널 pub/sub 통합 테스트")
class NotificationChannelPubSubIntegrationTest extends InfraModuleIntegrationTest {

    private static final String DESTINATION = "/topic/room/ABC123/gameState";

    @Autowired
    private GameNotificationChannel gameNotificationChannel;

    @Autowired
    private NotificationSinkFake sinkFake;

    @Autowired
    private Tracer tracer;

    @BeforeEach
    void clearDeliveries() {
        sinkFake.clear();
    }

    @Test
    @DisplayName("발행한 알림이 구독자를 거쳐 sink에 그대로 도착한다")
    void 발행한_알림이_sink에_도착한다() throws Exception {
        // given
        sinkFake.expectDeliveries(1);

        // when
        gameNotificationChannel.publish(DESTINATION, new PayloadDummy("PLAYING"));

        // then
        assertThat(sinkFake.awaitDelivery(5)).isTrue();
        assertThat(sinkFake.deliveries()).singleElement()
                .satisfies(delivered -> {
                    assertThat(delivered.destination()).isEqualTo(DESTINATION);
                    assertThat(delivered.payloadJson()).isEqualTo("{\"state\":\"PLAYING\"}");
                });
    }

    @Test
    @DisplayName("발행 시점의 트레이스가 수신 측 consumer 스팬으로 이어진다")
    void 발행_트레이스가_수신_측으로_이어진다() throws Exception {
        // given
        sinkFake.expectDeliveries(1);
        final Span span = tracer.nextSpan().name("알림_발행").start();

        // when
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            gameNotificationChannel.publish(DESTINATION, new PayloadDummy("PLAYING"));
        } finally {
            span.end();
        }

        // then
        assertThat(sinkFake.awaitDelivery(5)).isTrue();
        assertThat(sinkFake.deliveries()).singleElement()
                .satisfies(delivered ->
                        assertThat(delivered.traceId()).isEqualTo(span.context().traceId()));
    }

    private record PayloadDummy(String state) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class NotificationSinkTestConfig {

        /**
         * {@link NotificationSink} 타입 빈은 이것 하나여야 한다 — 구독자가
         * {@code ObjectProvider.getIfAvailable()}로 받으므로 후보가 둘이면 기동이 실패한다.
         */
        @Bean
        NotificationSinkFake notificationSinkFake(Tracer tracer) {
            return new NotificationSinkFake(() -> {
                final Span currentSpan = tracer.currentSpan();
                return currentSpan == null ? null : currentSpan.context().traceId();
            });
        }
    }
}
