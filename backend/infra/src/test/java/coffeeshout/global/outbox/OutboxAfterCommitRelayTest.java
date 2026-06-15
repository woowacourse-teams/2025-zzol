package coffeeshout.global.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.support.StubBaseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * OutboxAfterCommitRelay 단위 테스트.
 * <p>
 * AFTER_COMMIT 리스너의 핵심 동작을 검증한다:
 * - Redis 발행 성공 시 → markPublished() 호출
 * - Redis 발행 실패 시 → 예외를 삼키고 PENDING 상태 유지 (Worker가 재시도)
 * - 역직렬화 실패 시 → 예외를 삼키고 PENDING 상태 유지
 */
@ExtendWith(MockitoExtension.class)
class OutboxAfterCommitRelayTest {

    @Mock
    private StreamPublisher streamPublisher;

    @Mock
    private OutboxEventProcessor eventProcessor;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxAfterCommitRelay outboxAfterCommitRelay;

    @Nested
    class onOutboxSaved_메서드는 {

        @Test
        void Redis_발행_성공_시_저장된_traceparent와_함께_발행하고_markPublished를_호출한다() throws Exception {
            // given
            BaseEvent mockEvent = new StubBaseEvent();
            String traceparent = "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01";
            OutboxSavedEvent savedEvent = new OutboxSavedEvent(1L, "room", "{\"@type\":\"StubBaseEvent\"}", traceparent);

            BDDMockito.given(objectMapper.readValue(savedEvent.payload(), BaseEvent.class))
                    .willReturn(mockEvent);

            // when
            outboxAfterCommitRelay.onOutboxSaved(savedEvent);

            // then
            verify(streamPublisher).publish(eq("room"), eq(savedEvent.payload()), eq(traceparent));
            verify(eventProcessor).markPublished(1L);
        }

        @Test
        void Redis_발행_실패_시_예외를_삼키고_markPublished를_호출하지_않는다() throws Exception {
            // given
            BaseEvent mockEvent = new StubBaseEvent();
            OutboxSavedEvent savedEvent = new OutboxSavedEvent(1L, "room", "{\"@type\":\"StubBaseEvent\"}", null);

            BDDMockito.given(objectMapper.readValue(savedEvent.payload(), BaseEvent.class))
                    .willReturn(mockEvent);
            doThrow(new RuntimeException("Redis connection refused"))
                    .when(streamPublisher).publish(any(String.class), any(String.class), nullable(String.class));

            // when — 예외가 밖으로 나가면 안 된다
            outboxAfterCommitRelay.onOutboxSaved(savedEvent);

            // then — markPublished가 호출되지 않아야 한다 (PENDING 유지)
            verify(eventProcessor, never()).markPublished(any());
        }

        @Test
        void 역직렬화_실패_시_예외를_삼키고_markPublished를_호출하지_않는다() throws Exception {
            // given
            OutboxSavedEvent savedEvent = new OutboxSavedEvent(1L, "room", "invalid-json", null);

            BDDMockito.given(objectMapper.readValue(savedEvent.payload(), BaseEvent.class))
                    .willThrow(new JsonProcessingException("역직렬화 실패") {});

            // when — 예외가 밖으로 나가면 안 된다
            outboxAfterCommitRelay.onOutboxSaved(savedEvent);

            // then — 역직렬화 실패 시에도 markPublished와 publish가 호출되지 않아야 한다
            verify(eventProcessor, never()).markPublished(any());
            verify(streamPublisher, never()).publish(any(String.class), any(String.class), nullable(String.class));
        }
    }
}
