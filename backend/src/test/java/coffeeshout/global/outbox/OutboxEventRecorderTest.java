package coffeeshout.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.ServiceTest;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * OutboxEventRecorder 단위 테스트.
 * <p>
 * ServiceTest가 @Transactional이므로 테스트 종료 시 롤백된다.
 * 따라서 @TransactionalEventListener(AFTER_COMMIT)은 실행되지 않는다.
 * AFTER_COMMIT 즉시 발행 동작은 OutboxAfterCommitRelayTest에서 별도 검증한다.
 */
class OutboxEventRecorderTest extends ServiceTest {

    @Autowired
    private OutboxEventRecorder outboxEventRecorder;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private StreamPublisher streamPublisher;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAllInBatch();
    }

    @Test
    void record_호출_시_Outbox_테이블에_PENDING_상태로_이벤트가_저장된다() {
        // given
        BaseEvent event = new PlayerListUpdateEvent("test-join-code");

        // when
        outboxEventRecorder.record(StreamKey.ROOM_BROADCAST, event);

        // then
        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);

        OutboxEvent saved = events.getFirst();
        assertThat(saved.getStreamKey()).isEqualTo("room");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getPayload()).contains("test-join-code");
    }

    @Test
    void record는_트랜잭션_커밋_전에는_Redis를_호출하지_않는다() {
        // given
        BaseEvent event = new PlayerListUpdateEvent("test-join-code");

        // when
        outboxEventRecorder.record(StreamKey.ROOM_BROADCAST, event);

        // then — @Transactional 테스트이므로 커밋 안 됨 → AFTER_COMMIT 미실행 → Redis 미호출
        Mockito.verifyNoInteractions(streamPublisher);
    }
}
