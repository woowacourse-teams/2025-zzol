package coffeeshout.admin.system.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import coffeeshout.admin.system.domain.DeadLetterSource;
import coffeeshout.admin.system.domain.MigrationHistoryRepository;
import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.global.outbox.OutboxEvent;
import coffeeshout.global.outbox.OutboxEventRepository;
import coffeeshout.global.outbox.OutboxStatus;
import coffeeshout.settlement.infra.persistence.SettlementDeadLetterEntity;
import coffeeshout.settlement.infra.persistence.SettlementDeadLetterJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("SystemService")
@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private SettlementDeadLetterJpaRepository settlementDeadLetterRepository;

    @Mock
    private MigrationHistoryRepository migrationHistoryRepository;

    @InjectMocks
    private SystemService systemService;

    @Nested
    class countDeadLetters {

        @Test
        void 두_큐를_더한다() {
            // 화면이 두 숫자를 머리로 더하게 두지 않는다. 운영자가 알고 싶은 것은
            // "지금 막힌 게 있나" 하나다.
            given(outboxEventRepository.countByStatus(OutboxStatus.DEAD_LETTER)).willReturn(3L);
            given(settlementDeadLetterRepository.count()).willReturn(2L);

            assertThat(systemService.countDeadLetters()).isEqualTo(5L);
        }
    }

    private static OutboxEvent deadLetter() {
        final OutboxEvent event = OutboxEvent.create("settlement:result", "{}");
        event.markDeadLetter();
        return event;
    }

    @Nested
    class requeue {

        @Test
        void 격리_상태를_PENDING_으로_되돌린다() {
            final OutboxEvent event = OutboxEvent.create("settlement:result", "{}");
            event.markDeadLetter();
            given(outboxEventRepository.findById(1L)).willReturn(Optional.of(event));

            systemService.requeue(1L);

            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        }

        @Test
        void 재시도_횟수는_되돌리지_않는다() {
            // 0으로 초기화하면 원인을 못 고친 채 다시 넣었을 때 열 번을 새로 쓴다.
            // 같은 메시지가 큐를 오래 도는 것보다 빨리 격리되는 편이 낫다.
            final OutboxEvent event = OutboxEvent.create("settlement:result", "{}");
            event.incrementRetryCount();
            event.incrementRetryCount();
            event.markDeadLetter();
            given(outboxEventRepository.findById(1L)).willReturn(Optional.of(event));

            systemService.requeue(1L);

            assertThat(event.getRetryCount()).isEqualTo(2);
        }

        @Test
        void 격리_상태가_아니면_거절한다() {
            // 아직 발행 대기 중인 메시지를 다시 넣으면 같은 내용이 두 번 흘러간다.
            final OutboxEvent pending = OutboxEvent.create("settlement:result", "{}");
            given(outboxEventRepository.findById(1L)).willReturn(Optional.of(pending));

            assertThatThrownBy(() -> systemService.requeue(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        void 없는_메시지는_거절한다() {
            given(outboxEventRepository.findById(404L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> systemService.requeue(404L)).isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    class discard {

        @Test
        void outbox_는_outbox_에서만_지운다() {
            final OutboxEvent deadLetter = deadLetter();
            given(outboxEventRepository.findById(1L)).willReturn(Optional.of(deadLetter));

            systemService.discard(DeadLetterSource.OUTBOX, 1L);

            then(outboxEventRepository).should().delete(deadLetter);
            then(settlementDeadLetterRepository).should(never()).delete(any());
        }

        @Test
        void 격리_상태가_아닌_outbox_는_지우지_않는다() {
            // outbox 테이블에는 발행을 기다리는 행이 함께 산다. id 만 보고 지우면 아직
            // 나가지 않은 도메인 이벤트가 사라지고, 그건 outbox 를 둔 이유를 깨뜨린다.
            final OutboxEvent pending = OutboxEvent.create("settlement:result", "{}");
            given(outboxEventRepository.findById(1L)).willReturn(Optional.of(pending));

            assertThatThrownBy(() -> systemService.discard(DeadLetterSource.OUTBOX, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PENDING");
            then(outboxEventRepository).should(never()).delete(any());
        }

        @Test
        void 없는_outbox_메시지는_거절한다() {
            given(outboxEventRepository.findById(404L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> systemService.discard(DeadLetterSource.OUTBOX, 404L))
                    .isInstanceOf(BusinessException.class);
            then(outboxEventRepository).should(never()).delete(any());
        }

        @Test
        void 정산은_정산에서만_지운다() {
            final SettlementDeadLetterEntity entity = new SettlementDeadLetterEntity("record-1", "사유", "{}");
            given(settlementDeadLetterRepository.findById(1L)).willReturn(Optional.of(entity));

            systemService.discard(DeadLetterSource.SETTLEMENT, 1L);

            then(settlementDeadLetterRepository).should().delete(entity);
            then(outboxEventRepository).should(never()).delete(any());
        }

        @Test
        void 없는_정산_메시지는_거절한다() {
            // deleteById 는 없는 행을 조용히 넘긴다. 그대로 두면 일어나지 않은 삭제가
            // 감사 로그에 성공으로 남는다.
            given(settlementDeadLetterRepository.findById(404L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> systemService.discard(DeadLetterSource.SETTLEMENT, 404L))
                    .isInstanceOf(BusinessException.class);
            then(settlementDeadLetterRepository).should(never()).delete(any());
        }
    }

    @Nested
    class 재처리_가능_여부 {

        @Test
        void 정산_DLQ_는_다시_넣을_수_없다() {
            // 이 테이블은 재처리가 아니라 사후 분석이 목적이다(SettlementDeadLetterEntity 주석).
            // 정산은 중복 반영이 곧 잘못된 정산이라 다시 흘려보내는 경로를 두지 않는다.
            assertThat(DeadLetterSource.SETTLEMENT.isRequeueable()).isFalse();
            assertThat(DeadLetterSource.OUTBOX.isRequeueable()).isTrue();
        }
    }
}
