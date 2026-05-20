package coffeeshout.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.redis.BaseEvent;
import coffeeshout.redis.stream.StreamPublisher;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxRelayWorkerTest {

    @Mock
    private OutboxEventProcessor eventProcessor;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private StreamPublisher streamPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxRelayWorker outboxRelayWorker;

    private OutboxEvent createMockEvent(Long id, String streamKey, String payload) {
        OutboxEvent event = OutboxEvent.create(streamKey, payload);
        try {
            Field idField = OutboxEvent.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(event, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return event;
    }

    @Nested
    class relay_메서드 {

        @Test
        void PENDING_이벤트가_없으면_아무것도_하지_않는다() {
            // given
            given(eventProcessor.fetchAndMarkInProgress(50)).willReturn(Collections.emptyList());

            // when
            outboxRelayWorker.relay();

            // then
            verify(streamPublisher, never()).publish(any(String.class), any());
            verify(eventProcessor, never()).markPublished(any());
        }

        @Test
        void 발행_성공_시_markPublished를_호출한다() throws Exception {
            // given
            OutboxEvent event = createMockEvent(1L, "room", "{\"@type\":\"PlayerListUpdateEvent\"}");
            given(eventProcessor.fetchAndMarkInProgress(50)).willReturn(List.of(event));
            given(objectMapper.readValue(event.getPayload(), BaseEvent.class))
                    .willReturn(new PlayerListUpdateEvent("test"));

            // when
            outboxRelayWorker.relay();

            // then
            verify(eventProcessor).fetchAndMarkInProgress(50);
            verify(streamPublisher).publish(eq("room"), any());
            verify(eventProcessor).markPublished(1L);
        }

        @Test
        void Redis_발행_실패_시_handleFailure를_호출한다() throws Exception {
            // given
            OutboxEvent event = createMockEvent(1L, "room", "{\"@type\":\"PlayerListUpdateEvent\"}");
            given(eventProcessor.fetchAndMarkInProgress(50)).willReturn(List.of(event));
            given(objectMapper.readValue(event.getPayload(), BaseEvent.class))
                    .willReturn(new PlayerListUpdateEvent("test"));
            doThrow(new RuntimeException("Redis connection refused"))
                    .when(streamPublisher).publish(any(String.class), any());

            // when
            outboxRelayWorker.relay();

            // then
            verify(eventProcessor).handleFailure(1L);
            verify(eventProcessor, never()).markPublished(any());
        }

        @Test
        void 배치_내_일부_실패해도_나머지는_정상_처리된다() throws Exception {
            // given
            OutboxEvent event1 = createMockEvent(1L, "room", "{\"@type\":\"event1\"}");
            OutboxEvent event2 = createMockEvent(2L, "room", "{\"@type\":\"event2\"}");
            OutboxEvent event3 = createMockEvent(3L, "room", "{\"@type\":\"event3\"}");

            given(eventProcessor.fetchAndMarkInProgress(50))
                    .willReturn(List.of(event1, event2, event3));

            BaseEvent mockEvent = new PlayerListUpdateEvent("test");
            given(objectMapper.readValue(any(String.class), eq(BaseEvent.class)))
                    .willReturn(mockEvent);

            doThrow(new RuntimeException("timeout"))
                    .doNothing()
                    .doNothing()
                    .when(streamPublisher).publish(any(String.class), any());

            // when
            outboxRelayWorker.relay();

            // then — event1은 실패 처리, event2와 event3은 성공 처리
            verify(eventProcessor).handleFailure(1L);
            verify(eventProcessor).markPublished(2L);
            verify(eventProcessor).markPublished(3L);
        }
    }
}
