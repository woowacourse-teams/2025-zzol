package coffeeshout.numberpoker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffeeshout.cardgame.application.port.EarlyFinishTrigger;
import coffeeshout.cardgame.application.port.FlowHandle;
import coffeeshout.fixture.PlayerFixture;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.numberpoker.application.port.NumberPokerFlowScheduler;
import coffeeshout.numberpoker.config.NumberPokerTimingProperties;
import coffeeshout.numberpoker.domain.DeckShuffler;
import coffeeshout.numberpoker.domain.NumberPokerGame;
import coffeeshout.numberpoker.domain.NumberPokerProbabilityAdjuster;
import coffeeshout.numberpoker.domain.PokerPhase;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.service.RoomQueryService;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NumberPokerFlowOrchestratorTest {

    private NumberPokerFlowOrchestrator orchestrator;
    private SynchronousFlowScheduler scheduler;
    private NumberPokerNotifier notifier;
    private ApplicationEventPublisher eventPublisher;
    private NumberPokerTimingProperties timing;
    private NumberPokerProbabilityAdjuster adjuster;
    private RoomQueryService roomQueryService;
    private DeckShuffler deckShuffler;

    Player 꾹이 = PlayerFixture.호스트꾹이();
    Player 루키 = PlayerFixture.게스트루키();

    @BeforeEach
    void setUp() {
        scheduler = new SynchronousFlowScheduler();
        notifier = mock(NumberPokerNotifier.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        roomQueryService = mock(RoomQueryService.class);
        timing = new NumberPokerTimingProperties(
                Duration.ofMillis(1), Duration.ofMillis(1), Duration.ofMillis(1),
                Duration.ofMillis(1), Duration.ofMillis(1), Duration.ofMillis(1),
                Duration.ofMillis(1));
        adjuster = new NumberPokerProbabilityAdjuster(0.3, 0.6);
        deckShuffler = deck -> Collections.shuffle(deck, new Random());

        orchestrator = new NumberPokerFlowOrchestrator(
                scheduler, timing, notifier, adjuster, eventPublisher, roomQueryService, deckShuffler);
    }

    @Nested
    class 정상_플로우_1라운드 {

        @Test
        void 단일_라운드_게임_완료_시_최종_페이즈는_DONE이다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.configureRoundCount(1);
            Room room = stubRoom("ABCD");

            orchestrator.startFlow(game, room);

            assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.DONE);
        }

        @Test
        void 게임_완료_시_DONE_페이즈로_notifier가_호출된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.configureRoundCount(1);
            Room room = stubRoom("ABCD");

            orchestrator.startFlow(game, room);

            ArgumentCaptor<NumberPokerGame> gameCaptor = ArgumentCaptor.forClass(NumberPokerGame.class);
            verify(notifier, atLeastOnce()).notifyPhaseChanged(gameCaptor.capture(), any());
            assertThat(gameCaptor.getAllValues())
                    .anyMatch(g -> g.getCurrentPhase() == PokerPhase.DONE);
        }

        @Test
        void 단일_라운드_게임_완료_후_notifier가_호출된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.configureRoundCount(1);
            Room room = stubRoom("ABCD");

            orchestrator.startFlow(game, room);

            verify(notifier, atLeastOnce()).notifyPhaseChanged(any(), any());
        }

        @Test
        void 단일_라운드_게임_완료_후_MiniGameFinishedEvent가_발행된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.configureRoundCount(1);
            Room room = stubRoom("ABCD");

            orchestrator.startFlow(game, room);

            verify(eventPublisher).publishEvent(any(MiniGameFinishedEvent.class));
        }

        @Test
        void 다중_라운드_게임_완료_후_MiniGameFinishedEvent는_한_번만_발행된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.configureRoundCount(3);
            Room room = stubRoom("ABCD");

            orchestrator.startFlow(game, room);

            verify(eventPublisher).publishEvent(any(MiniGameFinishedEvent.class));
        }

        @Test
        void 게임_완료_시_발행되는_MiniGameFinishedEvent에_올바른_joinCode와_게임타입이_담긴다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.configureRoundCount(1);
            Room room = stubRoom("ABCD");

            orchestrator.startFlow(game, room);

            ArgumentCaptor<MiniGameFinishedEvent> captor = ArgumentCaptor.forClass(MiniGameFinishedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            MiniGameFinishedEvent event = captor.getValue();
            assertThat(event.joinCode()).isEqualTo("ABCD");
            assertThat(event.miniGameType()).isEqualTo("NUMBER_POKER");
        }
    }

    @Nested
    class 전원_폴드_시_STAGE_2_스킵 {

        @Test
        void STAGE_1에서_전원_폴드_시_STAGE_2를_건너뛰고_SHOWDOWN을_거쳐_DONE이_된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.configureRoundCount(1);
            Room room = stubRoom("ABCD");

            // 스케줄러가 STAGE_1 종료 시점 액션을 지연 실행하기 전에 폴드
            scheduler.onBeforeStage1End(() -> {
                game.fold(꾹이);
                game.fold(루키);
            });

            orchestrator.startFlow(game, room);

            // 전원 폴드여도 SHOWDOWN(딜러 패 전체 공개)은 거친다 — 딜러 카드 2장 모두 공개됨으로 검증
            assertThat(game.getDealerVisibleCards()).hasSize(2);
            assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.DONE);
        }
    }

    @Nested
    class 전원_레디_조기_종료 {

        @Test
        void 전원_레디_트리거_발동_시_ROUND_READY가_즉시_종료된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.configureRoundCount(2);
            Room room = stubRoom("ABCD");

            // 첫 라운드 ROUND_READY에서 즉시 트리거
            scheduler.triggerReadyImmediately();

            orchestrator.startFlow(game, room);

            // 2라운드까지 완료된 상태여야 함
            assertThat(game.getCurrentPhase()).isEqualTo(PokerPhase.DONE);
            assertThat(game.isLastRound()).isTrue();
        }
    }

    @Nested
    class triggerEarlyRoundReady {

        @Test
        void triggerEarlyRoundReady_호출_시_저장된_트리거가_완료된다() {
            NumberPokerGame game = new NumberPokerGame(List.of(꾹이, 루키));
            game.configureRoundCount(2);
            Room room = stubRoom("ABCD");

            orchestrator.startFlow(game, room);
            assertThatCode(() -> orchestrator.triggerEarlyRoundReady("ABCD")).doesNotThrowAnyException();
        }
    }

    private Room stubRoom(String joinCode) {
        Room room = mock(Room.class);
        when(room.getJoinCode()).thenReturn(new JoinCode(joinCode));
        when(room.getPlayers()).thenReturn(List.of(꾹이, 루키));
        when(roomQueryService.getByJoinCode(any())).thenReturn(room);
        return room;
    }

    /**
     * 딜레이 없이 즉시 실행하는 동기식 FlowScheduler.
     * STAGE_1 종료 액션 전 콜백과 readyTrigger 즉시 완료 옵션을 지원한다.
     */
    static class SynchronousFlowScheduler implements NumberPokerFlowScheduler {

        private Runnable beforeStage1EndHook;
        private boolean triggerReadyImmediately = false;

        void onBeforeStage1End(Runnable hook) {
            this.beforeStage1EndHook = hook;
        }

        void triggerReadyImmediately() {
            this.triggerReadyImmediately = true;
        }

        @Override
        public FlowHandle schedule(Runnable action, Duration delay) {
            action.run();
            return new SynchronousFlowHandle(this, false);
        }

        @Override
        public EarlyFinishTrigger createEarlyFinishTrigger() {
            return new ImmediateEarlyFinishTrigger(triggerReadyImmediately);
        }

        Runnable getBeforeStage1EndHook() {
            return beforeStage1EndHook;
        }
    }

    static class SynchronousFlowHandle implements FlowHandle {

        private final SynchronousFlowScheduler scheduler;
        private final boolean isStage1DelayHandle;

        SynchronousFlowHandle(SynchronousFlowScheduler scheduler, boolean isStage1DelayHandle) {
            this.scheduler = scheduler;
            this.isStage1DelayHandle = isStage1DelayHandle;
        }

        @Override
        public FlowHandle andThen(Runnable action, Duration delay) {
            // stage1 딜레이 완료 시점에 훅 실행
            if (isStage1DelayHandle && scheduler.getBeforeStage1EndHook() != null) {
                scheduler.getBeforeStage1EndHook().run();
            }
            action.run();
            return new SynchronousFlowHandle(scheduler, false);
        }

        @Override
        public FlowHandle raceTimeout(Duration timeout, EarlyFinishTrigger trigger, Duration earlyFinishExtraDelay) {
            // trigger가 완료된 경우(즉시 종료) 또는 타임아웃 - 둘 다 즉시 다음으로 진행
            return new SynchronousFlowHandle(scheduler, false);
        }

        @Override
        public FlowHandle onError(java.util.function.Consumer<Throwable> errorHandler) {
            return this;
        }
    }

    static class ImmediateEarlyFinishTrigger implements EarlyFinishTrigger {

        private boolean completed;

        ImmediateEarlyFinishTrigger(boolean completedImmediately) {
            this.completed = completedImmediately;
        }

        @Override
        public void complete() {
            this.completed = true;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public java.util.concurrent.CompletionStage<Void> asCompletionStage() {
            if (completed) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            return new java.util.concurrent.CompletableFuture<>();
        }
    }
}
