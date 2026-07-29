package coffeeshout.settlement.infra.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.StreamInfo.XInfoGroups;
import coffeeshout.settlement.infra.persistence.SettlementDeadLetterJpaRepository;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class SettlementConsumerMetricsTest {

    private MeterRegistry meterRegistry;
    private StringRedisTemplate stringRedisTemplate;
    private StreamOperations<String, Object, Object> streamOperations;
    private SettlementDeadLetterJpaRepository deadLetterRepository;
    private SettlementConsumerMetrics metrics;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        stringRedisTemplate = mock(StringRedisTemplate.class);
        streamOperations = mock(StreamOperations.class);
        given(stringRedisTemplate.opsForStream()).willReturn(streamOperations);
        deadLetterRepository = mock(SettlementDeadLetterJpaRepository.class);
        metrics = new SettlementConsumerMetrics(stringRedisTemplate, meterRegistry, deadLetterRepository);
    }

    @Test
    void 그룹_pending_lag_컨슈머수_DLQ길이_게이지가_등록된다() {
        metrics.initializeMetrics();

        assertThat(meterRegistry.find("redis.stream.group.pending")
                .tag("group", SettlementStreamConsumer.GROUP).gauge()).isNotNull();
        assertThat(meterRegistry.find("redis.stream.group.lag")
                .tag("group", SettlementStreamConsumer.GROUP).gauge()).isNotNull();
        assertThat(meterRegistry.find("redis.stream.group.consumers")
                .tag("group", SettlementStreamConsumer.GROUP).gauge()).isNotNull();
        assertThat(meterRegistry.find("settlement.deadletter.count").gauge()).isNotNull();
    }

    @Test
    void 그룹_lag을_raw_응답에서_읽는다() {
        // XInfoGroup은 lag accessor가 없어 raw 응답 필드로 읽는다 — Redis 7+/Valkey가 반환한다
        given(streamOperations.groups(SettlementStreamConsumer.STREAM_KEY))
                .willReturn(XInfoGroups.fromList(List.of(그룹_응답("lag", 4L))));

        metrics.initializeMetrics();

        Gauge lag = meterRegistry.find("redis.stream.group.lag")
                .tag("group", SettlementStreamConsumer.GROUP).gauge();
        assertThat(lag.value()).isEqualTo(4.0);
    }

    @Test
    void lag_필드가_없으면_NaN을_반환한다() {
        // Redis 7 미만 응답 또는 XSETID 이후 산정 불가(null) — 게이지는 NaN으로 표시한다
        given(streamOperations.groups(SettlementStreamConsumer.STREAM_KEY))
                .willReturn(XInfoGroups.fromList(List.of(그룹_응답_lag_없음())));

        metrics.initializeMetrics();

        Gauge lag = meterRegistry.find("redis.stream.group.lag")
                .tag("group", SettlementStreamConsumer.GROUP).gauge();
        assertThat(lag.value()).isNaN();
    }

    private List<Object> 그룹_응답(String extraKey, Object extraValue) {
        return List.of(
                "name", SettlementStreamConsumer.GROUP,
                "consumers", 1L,
                "pending", 2L,
                "last-delivered-id", "0-0",
                extraKey, extraValue
        );
    }

    private List<Object> 그룹_응답_lag_없음() {
        return List.of(
                "name", SettlementStreamConsumer.GROUP,
                "consumers", 1L,
                "pending", 2L,
                "last-delivered-id", "0-0"
        );
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
    void DLQ_건수를_DB에서_읽는다() {
        given(deadLetterRepository.count()).willReturn(3L);

        metrics.initializeMetrics();

        Gauge dlq = meterRegistry.find("settlement.deadletter.count").gauge();
        assertThat(dlq.value()).isEqualTo(3.0);
    }
}
