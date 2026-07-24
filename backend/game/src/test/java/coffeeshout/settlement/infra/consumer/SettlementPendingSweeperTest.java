package coffeeshout.settlement.infra.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.settlement.infra.consumer.SettlementMessageProcessor.PoisonMessageException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettlementPendingSweeperTest {

    @Mock
    StringRedisTemplate stringRedisTemplate;
    @Mock
    StreamOperations<String, Object, Object> streamOperations;
    @Mock
    SettlementMessageProcessor processor;
    @Mock
    SettlementDeadLetterPublisher deadLetterPublisher;

    private SettlementPendingSweeper sweeper;

    @BeforeEach
    void setUp() {
        given(stringRedisTemplate.opsForStream()).willReturn(streamOperations);
        sweeper = new SettlementPendingSweeper(
                stringRedisTemplate, processor, deadLetterPublisher, "test-consumer");
    }

    @Nested
    class pending_메시지를_회수할_때 {

        @Test
        void 유휴_시간이_기준_미만이면_회수하지_않는다() {
            // 살아있는 컨슈머가 처리 중인 메시지를 가로채면 안 된다
            pending_설정(대기_메시지("1-1", Duration.ofSeconds(5), 1));

            sweeper.sweep();

            verify(streamOperations, never()).claim(anyString(), anyString(), anyString(), any(XClaimOptions.class));
        }

        @Test
        void 유휴_기준을_넘긴_메시지는_회수해_재처리하고_ACK한다() {
            pending_설정(대기_메시지("1-1", Duration.ofMinutes(2), 2));
            MapRecord<String, String, String> record = 회수_레코드_설정("1-1");

            sweeper.sweep();

            verify(processor).process(record);
            verify(streamOperations).acknowledge(
                    eq(SettlementStreamConsumer.STREAM_KEY), eq(SettlementStreamConsumer.GROUP),
                    eq(RecordId.of("1-1")));
        }

        @Test
        void 최대_재전달_횟수를_소진한_메시지는_DLQ로_격리하고_재처리하지_않는다() {
            pending_설정(대기_메시지("1-1", Duration.ofMinutes(2), SettlementPendingSweeper.MAX_DELIVERIES));
            MapRecord<String, String, String> record = 회수_레코드_설정("1-1");

            sweeper.sweep();

            verify(deadLetterPublisher).publish(eq(record), anyString());
            verify(processor, never()).process(any());
            verify(streamOperations).acknowledge(
                    eq(SettlementStreamConsumer.STREAM_KEY), eq(SettlementStreamConsumer.GROUP),
                    eq(RecordId.of("1-1")));
        }

        @Test
        void 재처리가_실패하면_ACK하지_않고_다음_주기로_넘긴다() {
            // XCLAIM이 전달 횟수를 올리므로 반복 실패는 결국 MAX_DELIVERIES에 도달해 DLQ로 수렴한다
            pending_설정(대기_메시지("1-1", Duration.ofMinutes(2), 2));
            MapRecord<String, String, String> record = 회수_레코드_설정("1-1");
            willThrow(new RuntimeException("DB 일시 실패")).given(processor).process(record);

            sweeper.sweep();

            verify(streamOperations, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
        }

        @Test
        void 회수한_포이즌_메시지는_DLQ로_격리하고_ACK한다() {
            pending_설정(대기_메시지("1-1", Duration.ofMinutes(2), 2));
            MapRecord<String, String, String> record = 회수_레코드_설정("1-1");
            willThrow(new PoisonMessageException("파싱 실패")).given(processor).process(record);

            sweeper.sweep();

            verify(deadLetterPublisher).publish(eq(record), anyString());
            verify(streamOperations).acknowledge(
                    eq(SettlementStreamConsumer.STREAM_KEY), eq(SettlementStreamConsumer.GROUP),
                    eq(RecordId.of("1-1")));
        }

        @Test
        void 다른_스위퍼가_먼저_회수한_메시지는_건너뛴다() {
            // XCLAIM min-idle 경합에서 진 쪽은 빈 결과를 받는다
            pending_설정(대기_메시지("1-1", Duration.ofMinutes(2), 2));
            given(streamOperations.claim(anyString(), anyString(), anyString(), any(XClaimOptions.class)))
                    .willReturn(List.of());

            sweeper.sweep();

            verify(processor, never()).process(any());
            verify(streamOperations, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
        }
    }

    private PendingMessage 대기_메시지(String id, Duration idle, long deliveryCount) {
        return new PendingMessage(
                RecordId.of(id),
                Consumer.from(SettlementStreamConsumer.GROUP, "dead-consumer"),
                idle,
                deliveryCount
        );
    }

    private void pending_설정(PendingMessage... messages) {
        given(streamOperations.pending(
                eq(SettlementStreamConsumer.STREAM_KEY), eq(SettlementStreamConsumer.GROUP), any(), eq(50L)))
                .willReturn(new PendingMessages(SettlementStreamConsumer.GROUP, List.of(messages)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private MapRecord<String, String, String> 회수_레코드_설정(String id) {
        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .in(SettlementStreamConsumer.STREAM_KEY)
                .ofMap(Map.of("payload", "{}"))
                .withId(RecordId.of(id));
        given(streamOperations.claim(anyString(), anyString(), anyString(), any(XClaimOptions.class)))
                .willReturn((List) List.of(record));
        return record;
    }
}
