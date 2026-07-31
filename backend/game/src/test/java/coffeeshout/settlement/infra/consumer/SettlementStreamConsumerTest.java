package coffeeshout.settlement.infra.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.global.redis.config.RedisStreamContainerRegistry;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.settlement.infra.consumer.SettlementMessageProcessor.PoisonMessageException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettlementStreamConsumerTest {

    @Mock
    RedisConnectionFactory redisConnectionFactory;
    @Mock
    StringRedisTemplate stringRedisTemplate;
    @Mock
    StreamOperations<String, Object, Object> streamOperations;
    @Mock
    RedisStreamProperties properties;
    @Mock
    RedisStreamContainerRegistry containerRegistry;
    @Mock
    SettlementMessageProcessor processor;
    @Mock
    SettlementDeadLetterPublisher deadLetterPublisher;
    @Mock
    ThreadPoolTaskExecutor executor;

    private SettlementStreamConsumer consumer;

    @BeforeEach
    void setUp() {
        given(stringRedisTemplate.opsForStream()).willReturn(streamOperations);
        consumer = new SettlementStreamConsumer(
                redisConnectionFactory,
                stringRedisTemplate,
                properties,
                containerRegistry,
                processor,
                deadLetterPublisher,
                executor,
                "test-consumer"
        );
    }

    @Nested
    class 스트림_오류를_처리할_때 {

        @Test
        void NOGROUP이_cause에_감싸여_와도_그룹을_재생성한다() {
            // Spring이 Lettuce 예외를 RedisSystemException("Error in execution")으로 감싸면
            // 최상위 메시지에 NOGROUP이 없다 — cause 체인을 못 보면 자가 복구가 죽고
            // 폴링 루프가 에러 스택 로그를 무한 반복한다(테스트 결과 XML 177MB 사고)
            RedisSystemException wrapped = new RedisSystemException(
                    "Error in execution",
                    new RuntimeException("NOGROUP No such key 'settlement:result' or consumer group 'settlement'")
            );

            consumer.handleStreamError(wrapped);

            verify(stringRedisTemplate).execute(any(RedisCallback.class), eq(true));
        }

        @Test
        void NOGROUP이_아닌_오류는_그룹을_재생성하지_않는다() {
            consumer.handleStreamError(new RuntimeException("connection reset"));

            verify(stringRedisTemplate, never()).execute(any(RedisCallback.class), eq(true));
        }
    }

    @Nested
    class 메시지를_수신할_때 {

        @Test
        void 처리에_성공하면_ACK한다() {
            MapRecord<String, String, String> record = 레코드();

            consumer.onMessage(record);

            verify(processor).process(record);
            verify(streamOperations).acknowledge(
                    SettlementStreamConsumer.STREAM_KEY, SettlementStreamConsumer.GROUP, record.getId());
        }

        @Test
        void 포이즌_메시지는_DLQ로_격리하고_ACK해_재전달_루프를_끊는다() {
            MapRecord<String, String, String> record = 레코드();
            willThrow(new PoisonMessageException("파싱 실패")).given(processor).process(record);

            consumer.onMessage(record);

            verify(deadLetterPublisher).publish(eq(record), anyString());
            verify(streamOperations).acknowledge(
                    SettlementStreamConsumer.STREAM_KEY, SettlementStreamConsumer.GROUP, record.getId());
        }

        @Test
        void 일시_실패는_ACK하지_않아_PEL에_남긴다() {
            // ACK 없는 실패는 스위퍼가 회수한다 — 멱등성은 정산 원장이 보장하므로 재처리가 안전하다
            MapRecord<String, String, String> record = 레코드();
            willThrow(new RuntimeException("DB 일시 실패")).given(processor).process(record);

            consumer.onMessage(record);

            verify(streamOperations, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
            verify(deadLetterPublisher, never()).publish(any(), anyString());
        }
    }

    private MapRecord<String, String, String> 레코드() {
        return StreamRecords.newRecord()
                .in(SettlementStreamConsumer.STREAM_KEY)
                .ofMap(Map.of("payload", "{}"))
                .withId(RecordId.of("1-1"));
    }
}
