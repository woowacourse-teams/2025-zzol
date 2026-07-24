package coffeeshout.settlement.infra.consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.verify;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import coffeeshout.settlement.application.SeasonLeaderboardService;
import coffeeshout.settlement.application.SettlementService;
import coffeeshout.settlement.application.SettlementService.SettledScore;
import coffeeshout.settlement.domain.SeasonTier;
import coffeeshout.settlement.event.SettlementResultEvent;
import coffeeshout.settlement.event.SettlementResultEvent.PlayerResult;
import coffeeshout.settlement.infra.consumer.SettlementMessageProcessor.PoisonMessageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettlementMessageProcessorTest {

    @Mock
    StreamTracePropagator streamTracePropagator;
    @Mock
    SettlementService settlementService;
    @Mock
    SeasonLeaderboardService leaderboardService;

    private ObjectMapper objectMapper;
    private SettlementMessageProcessor processor;

    /** 정산 스트림에 잘못 흘러든 다른 타입을 흉내내는 더미 이벤트 */
    record OtherEventDummy(String eventId, Instant timestamp) implements BaseEvent {
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerSubtypes(SettlementResultEvent.class, OtherEventDummy.class);

        processor = new SettlementMessageProcessor(
                objectMapper, streamTracePropagator, settlementService, leaderboardService);

        // 트레이스 스코프는 본문 실행만 위임한다
        willAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return null;
        }).given(streamTracePropagator).runInConsumerScope(any(), anyString(), any(Runnable.class));
    }

    @Test
    void 정산_이벤트를_파싱해_정산하고_리더보드를_갱신한다() throws Exception {
        SettlementResultEvent event = SettlementResultEvent.of(
                "AB3C", 7L, "BLIND_TIMER", List.of(new PlayerResult(1L, 1, 12L)));
        SettledScore settled = new SettledScore(1L, "2026-07", 100, SeasonTier.BRONZE);
        given(settlementService.settle(any())).willReturn(List.of(settled));

        processor.process(레코드(objectMapper.writeValueAsString(event)));

        verify(settlementService).settle(any(SettlementResultEvent.class));
        verify(leaderboardService).updateScore(settled);
    }

    @Test
    void payload가_없으면_포이즌으로_판정한다() {
        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .in("settlement:result")
                .ofMap(Map.of("other", "field"))
                .withId(RecordId.of("1-1"));

        assertThatThrownBy(() -> processor.process(record))
                .isInstanceOf(PoisonMessageException.class);
    }

    @Test
    void 파싱_불가능한_payload는_포이즌으로_판정한다() {
        assertThatThrownBy(() -> processor.process(레코드("{깨진 json")))
                .isInstanceOf(PoisonMessageException.class);
    }

    @Test
    void 정산_이벤트가_아닌_타입은_포이즌으로_판정한다() throws Exception {
        String otherPayload = objectMapper.writeValueAsString(new OtherEventDummy("id", Instant.now()));

        assertThatThrownBy(() -> processor.process(레코드(otherPayload)))
                .isInstanceOf(PoisonMessageException.class);
    }

    @Test
    void 정산_실패는_포이즌이_아니라_그대로_전파된다() throws Exception {
        // 일시 실패는 ACK 보류 → 재전달로 수렴해야 하므로 포이즌으로 오분류하면 안 된다
        SettlementResultEvent event = SettlementResultEvent.of(
                "AB3C", 7L, "BLIND_TIMER", List.of(new PlayerResult(1L, 1, 12L)));
        given(settlementService.settle(any())).willThrow(new RuntimeException("DB 일시 실패"));
        String payload = objectMapper.writeValueAsString(event);

        assertThatCode(() -> processor.process(레코드(payload)))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(PoisonMessageException.class);
    }

    private MapRecord<String, String, String> 레코드(String payload) {
        return StreamRecords.newRecord()
                .in("settlement:result")
                .ofMap(Map.of("payload", payload))
                .withId(RecordId.of("1-1"));
    }
}
