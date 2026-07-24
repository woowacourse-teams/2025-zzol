package coffeeshout.settlement.infra.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class SettlementConsumerMetricsTest {

    private MeterRegistry meterRegistry;
    private StringRedisTemplate stringRedisTemplate;
    private StreamOperations<String, Object, Object> streamOperations;
    private SettlementConsumerMetrics metrics;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        stringRedisTemplate = mock(StringRedisTemplate.class);
        streamOperations = mock(StreamOperations.class);
        given(stringRedisTemplate.opsForStream()).willReturn(streamOperations);
        metrics = new SettlementConsumerMetrics(stringRedisTemplate, meterRegistry);
    }

    @Test
    void 그룹_pending_컨슈머수_DLQ길이_게이지가_등록된다() {
        metrics.initializeMetrics();

        assertThat(meterRegistry.find("redis.stream.group.pending")
                .tag("group", SettlementStreamConsumer.GROUP).gauge()).isNotNull();
        assertThat(meterRegistry.find("redis.stream.group.consumers")
                .tag("group", SettlementStreamConsumer.GROUP).gauge()).isNotNull();
        assertThat(meterRegistry.find("redis.stream.length")
                .tag("stream", SettlementDeadLetterPublisher.DLQ_KEY).gauge()).isNotNull();
    }

    @Test
    void 그룹이_아직_없으면_NaN을_반환한다() {
        // 기동 직후·Redis 초기화 직후 — 게이지가 예외 대신 NaN으로 표시되어야 한다
        given(streamOperations.groups(SettlementStreamConsumer.STREAM_KEY))
                .willThrow(new RuntimeException("no such key"));

        metrics.initializeMetrics();

        Gauge pending = meterRegistry.find("redis.stream.group.pending")
                .tag("group", SettlementStreamConsumer.GROUP).gauge();
        assertThat(pending.value()).isNaN();
    }

    @Test
    void DLQ_길이를_XLEN으로_읽는다() {
        given(streamOperations.size(SettlementDeadLetterPublisher.DLQ_KEY)).willReturn(3L);

        metrics.initializeMetrics();

        Gauge dlq = meterRegistry.find("redis.stream.length")
                .tag("stream", SettlementDeadLetterPublisher.DLQ_KEY).gauge();
        assertThat(dlq.value()).isEqualTo(3.0);
    }
}
