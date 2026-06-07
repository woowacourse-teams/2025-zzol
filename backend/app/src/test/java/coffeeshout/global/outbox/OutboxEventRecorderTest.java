package coffeeshout.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.support.app.StreamMockedServiceTest;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import coffeeshout.room.infra.messaging.RoomStreamKey;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * OutboxEventRecorder 단위 테스트.
 * <p>
 * ServiceTest가 @Transactional이므로 테스트 종료 시 롤백된다.
 * 따라서 @TransactionalEventListener(AFTER_COMMIT)은 실행되지 않는다.
 * AFTER_COMMIT 즉시 발행 동작은 OutboxAfterCommitRelayTest에서 별도 검증한다.
 */
class OutboxEventRecorderTest extends StreamMockedServiceTest {

    @Autowired
    private OutboxEventRecorder outboxEventRecorder;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoSpyBean
    private StreamTracePropagator streamTracePropagator;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAllInBatch();
    }

    @Test
    void record_호출_시_Outbox_테이블에_PENDING_상태로_이벤트가_저장된다() {
        // given
        BaseEvent event = new PlayerListUpdateEvent("test-join-code");

        // when
        outboxEventRecorder.record(RoomStreamKey.BROADCAST, event);

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
    void record_호출_시_기록_시점의_traceparent가_함께_저장된다() {
        // given — 재시도 릴레이는 스케줄러 스레드라 컨텍스트가 없으므로 기록 시점 캡처를 검증한다
        String traceparent = "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01";
        BDDMockito.given(streamTracePropagator.currentTraceparent()).willReturn(traceparent);
        BaseEvent event = new PlayerListUpdateEvent("test-join-code");

        // when
        outboxEventRecorder.record(RoomStreamKey.BROADCAST, event);

        // then
        OutboxEvent saved = outboxEventRepository.findAll().getFirst();
        assertThat(saved.getTraceparent()).isEqualTo(traceparent);
    }

    @Test
    void 트레이스_컨텍스트가_없으면_traceparent가_null로_저장된다() {
        // given — 테스트 스레드에는 활성 스팬이 없다
        BaseEvent event = new PlayerListUpdateEvent("test-join-code");

        // when
        outboxEventRecorder.record(RoomStreamKey.BROADCAST, event);

        // then
        OutboxEvent saved = outboxEventRepository.findAll().getFirst();
        assertThat(saved.getTraceparent()).isNull();
    }

    @Test
    void record는_트랜잭션_커밋_전에는_Redis를_호출하지_않는다() {
        // given
        BaseEvent event = new PlayerListUpdateEvent("test-join-code");

        // when
        outboxEventRecorder.record(RoomStreamKey.BROADCAST, event);

        // then — @Transactional 테스트이므로 커밋 안 됨 → AFTER_COMMIT 미실행 → Redis 미호출
        Mockito.verifyNoInteractions(streamPublisher);
    }
}
