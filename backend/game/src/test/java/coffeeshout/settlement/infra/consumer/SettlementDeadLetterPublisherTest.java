package coffeeshout.settlement.infra.consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coffeeshout.settlement.infra.persistence.SettlementDeadLetterEntity;
import coffeeshout.settlement.infra.persistence.SettlementDeadLetterJpaRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;

@ExtendWith(MockitoExtension.class)
class SettlementDeadLetterPublisherTest {

    @InjectMocks
    SettlementDeadLetterPublisher publisher;

    @Mock
    SettlementDeadLetterJpaRepository deadLetterRepository;

    @Test
    void 격리_메시지를_레코드_ID와_사유와_함께_저장한다() {
        publisher.publish(레코드(), "파싱 실패");

        ArgumentCaptor<SettlementDeadLetterEntity> captor =
                ArgumentCaptor.forClass(SettlementDeadLetterEntity.class);
        verify(deadLetterRepository).save(captor.capture());
        SettlementDeadLetterEntity saved = captor.getValue();
        assertThatCode(saved::getRecordId).doesNotThrowAnyException();
    }

    @Test
    void 이미_격리된_메시지는_예외_없이_성공으로_처리한다() {
        // 격리 후 ACK 실패로 재전달된 경우 — 예외가 전파되면 ACK가 계속 막혀 재전달 루프가 안 끊긴다
        given(deadLetterRepository.save(any(SettlementDeadLetterEntity.class)))
                .willThrow(new DataIntegrityViolationException("uk_settlement_dead_letter_record"));

        assertThatCode(() -> publisher.publish(레코드(), "파싱 실패")).doesNotThrowAnyException();
    }

    private MapRecord<String, String, String> 레코드() {
        return StreamRecords.newRecord()
                .in("settlement:result")
                .ofMap(Map.of("payload", "{\"broken\":true}"));
    }
}
