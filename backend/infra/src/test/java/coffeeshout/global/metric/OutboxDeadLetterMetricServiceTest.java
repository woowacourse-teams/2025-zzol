package coffeeshout.global.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import coffeeshout.global.outbox.OutboxEventRepository;
import coffeeshout.global.outbox.OutboxStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxDeadLetterMetricServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void DEAD_LETTER_건수를_게이지로_노출한다() {
        given(outboxEventRepository.countByStatus(OutboxStatus.DEAD_LETTER)).willReturn(7L);
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new OutboxDeadLetterMetricService(outboxEventRepository, registry).initializeMetrics();

        final double value = registry.get("outbox.dead_letter.count").gauge().value();

        assertThat(value).isEqualTo(7.0);
    }

    @Test
    void 조회_실패시_NaN으로_안전하게_떨어진다() {
        given(outboxEventRepository.countByStatus(OutboxStatus.DEAD_LETTER))
                .willThrow(new RuntimeException("db down"));
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new OutboxDeadLetterMetricService(outboxEventRepository, registry).initializeMetrics();

        final double value = registry.get("outbox.dead_letter.count").gauge().value();

        assertThat(value).isNaN();
    }
}
