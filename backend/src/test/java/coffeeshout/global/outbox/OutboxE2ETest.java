package coffeeshout.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.cardgame.application.port.CardGameFlowScheduler;
import coffeeshout.fixture.TestContainerSupport;
import coffeeshout.global.outbox.OutboxE2ETest.OutboxE2ETestConfig;
import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.stream.StreamKey;
import coffeeshout.numberpoker.application.port.NumberPokerFlowScheduler;
import coffeeshout.room.domain.event.PlayerListUpdateEvent;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;

/**
 * Outbox 패턴 E2E 테스트 (2단 콤보 Outbox).
 * <p>
 * Testcontainers로 MySQL + Redis를 실제로 띄워서 전체 흐름을 검증한다.
 * <p>
 * 2단 콤보 구조: - 1단계: record() → 트랜잭션 커밋 → AFTER_COMMIT 즉시 발행 → PUBLISHED (Happy Path) - 2단계: 즉시 발행 실패 시 → PENDING 유지 →
 * Worker가 500ms 후 재시도 (Fallback)
 * <p>
 * H2 대신 MySQL을 쓰는 이유: FOR UPDATE SKIP LOCKED가 H2에서 지원되지 않는다.
 */

@SpringBootTest
@ActiveProfiles("test")
@Import(OutboxE2ETestConfig.class)
class OutboxE2ETest extends TestContainerSupport {

    @TestConfiguration
    static class OutboxE2ETestConfig {

        @Bean
        @Primary
        public CardGameFlowScheduler mockCardGameFlowScheduler() {
            return Mockito.mock(CardGameFlowScheduler.class);
        }

        @Bean
        @Primary
        public NumberPokerFlowScheduler mockNumberPokerFlowScheduler() {
            return Mockito.mock(NumberPokerFlowScheduler.class);
        }

        @Bean(name = "cardGameTaskScheduler")
        public TaskScheduler cardGameTaskScheduler() {
            return new coffeeshout.global.config.ShutDownTestScheduler();
        }

        @Bean(name = "delayRemovalScheduler")
        public TaskScheduler delayRemovalScheduler() {
            return new coffeeshout.global.config.ShutDownTestScheduler();
        }

        @Bean(name = "racingGameScheduler")
        public TaskScheduler racingGameScheduler() {
            return new coffeeshout.global.config.ShutDownTestScheduler();
        }

        @Bean(name = "speedTouchGameScheduler")
        public TaskScheduler speedTouchGameScheduler() {
            return new coffeeshout.global.config.ShutDownTestScheduler();
        }

        @Bean(name = "blindTimerGameScheduler")
        public TaskScheduler blindTimerGameScheduler() {
            return new coffeeshout.global.config.ShutDownTestScheduler();
        }

        @Bean(name = "bombRelayGameScheduler")
        public TaskScheduler bombRelayGameScheduler() {
            return new coffeeshout.global.config.ShutDownTestScheduler();
        }

        /**
         * 기본 taskScheduler를 no-op으로 덮어써서 @Scheduled 메서드 실행을 막는다.
         * OutboxRelayWorker의 relay(), recoverStaleEvents(), cleanup()이
         * 테스트 중에 백그라운드로 돌면서 수동 호출과 경합하는 걸 방지한다.
         * ShutDownTestScheduler는 ThreadPoolTaskScheduler 상속이라 실제로 태스크를 실행하므로
         * Mockito mock을 사용해 진짜 no-op으로 만든다.
         */
        @Bean(name = "taskScheduler")
        @Primary
        public TaskScheduler noOpTaskScheduler() {
            return Mockito.mock(TaskScheduler.class, Answers.RETURNS_MOCKS);
        }
    }

    @Autowired
    private OutboxEventRecorder outboxEventRecorder;

    @Autowired
    private OutboxRelayWorker outboxRelayWorker;

    @Autowired
    private OutboxEventProcessor outboxEventProcessor;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @Nested
    class AFTER_COMMIT_즉시_발행_Happy_Path는 {

        @Test
        void record_호출_후_트랜잭션_커밋_시_즉시_PUBLISHED로_전환된다() {
            // given & when — record() 호출 → 자체 @Transactional 커밋 → AFTER_COMMIT 즉시 발행
            final BaseEvent event = new PlayerListUpdateEvent("test-join-code");
            outboxEventRecorder.record(StreamKey.ROOM_BROADCAST, event);

            // then — AFTER_COMMIT이 즉시 실행되어 이미 PUBLISHED
            final List<OutboxEvent> events = outboxEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        }

        @Test
        void 여러_이벤트를_record하면_각각_즉시_PUBLISHED로_전환된다() {
            // given & when
            IntStream.range(0, 5).forEach(i -> {
                final BaseEvent event = new PlayerListUpdateEvent("join-code-" + i);
                outboxEventRecorder.record(StreamKey.ROOM_BROADCAST, event);
            });

            // then
            final List<OutboxEvent> events = outboxEventRepository.findAll();
            assertThat(events).hasSize(5).allSatisfy(event ->
                    assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED)
            );
        }
    }

    @Nested
    class Worker_폴링_재시도_Fallback는 {

        @Test
        void 직접_PENDING_레코드를_넣으면_Worker가_relay해서_PUBLISHED로_전환한다() {
            // given — Outbox에 직접 PENDING 레코드 삽입 (AFTER_COMMIT 우회)
            final OutboxEvent event = OutboxEvent.create("room",
                    "{\"@type\":\"PlayerListUpdateEvent\",\"eventId\":\"test\",\"timestamp\":\"2025-01-01T00:00:00Z\",\"joinCode\":\"ABCD\"}");
            outboxEventRepository.saveAndFlush(event);

            assertThat(outboxEventRepository.findAll().getFirst().getStatus()).isEqualTo(OutboxStatus.PENDING);

            // when — Worker 수동 호출
            outboxRelayWorker.relay();

            // then
            final OutboxEvent afterRelay = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(afterRelay.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        }

        @Test
        void Redis_발행_실패_시_retryCount가_증가하고_PENDING으로_복귀한다() {
            // given — 존재하지 않는 streamKey로 발행 실패를 유도
            final OutboxEvent event = OutboxEvent.create("nonexistent-stream-key", "{\"invalid\":true}");
            outboxEventRepository.saveAndFlush(event);

            // when
            outboxRelayWorker.relay();

            // then
            final OutboxEvent afterRelay = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(afterRelay.getRetryCount()).isEqualTo(1);
            assertThat(afterRelay.getStatus()).isEqualTo(OutboxStatus.PENDING);
        }

    }

    @Test
    void 재시도_10회_실패_시_DEAD_LETTER로_전환된다() {
        // given
        final OutboxEvent event = OutboxEvent.create("nonexistent-stream-key", "{\"invalid\":true}");
        outboxEventRepository.saveAndFlush(event);

        // when — 10번 relay 반복
        for (int i = 0; i < 10; i++) {
            outboxRelayWorker.relay();
        }

        // then
        final OutboxEvent afterRetries = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(afterRetries.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
        assertThat(afterRetries.getRetryCount()).isEqualTo(10);
    }

    @Nested
    class fetchAndMarkInProgress는 {

        @Test
        void PENDING_이벤트를_IN_PROGRESS로_전환한다() {
            // given — 직접 PENDING 레코드 삽입
            final OutboxEvent event = OutboxEvent.create("room", "{\"type\":\"test\"}");
            outboxEventRepository.saveAndFlush(event);

            // when
            final List<OutboxEvent> fetched = outboxEventProcessor.fetchAndMarkInProgress(50);

            // then
            assertThat(fetched).hasSize(1);
            assertThat(fetched.getFirst().getStatus()).isEqualTo(OutboxStatus.IN_PROGRESS);
        }

        @Test
        void IN_PROGRESS_이벤트는_다음_폴링에서_조회되지_않는다() {
            // given
            final OutboxEvent event = OutboxEvent.create("room", "{\"type\":\"test\"}");
            outboxEventRepository.saveAndFlush(event);
            outboxEventProcessor.fetchAndMarkInProgress(50);

            // when
            final List<OutboxEvent> secondFetch = outboxEventProcessor.fetchAndMarkInProgress(50);

            // then
            assertThat(secondFetch).isEmpty();
        }
    }
}
