package coffeeshout.numberpoker.application;

import coffeeshout.global.flow.EarlyFinishTrigger;
import coffeeshout.global.flow.FlowHandle;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.numberpoker.application.port.NumberPokerFlowScheduler;
import coffeeshout.numberpoker.config.NumberPokerTimingProperties;
import coffeeshout.numberpoker.domain.DeckShuffler;
import coffeeshout.numberpoker.domain.HandRanking;
import coffeeshout.numberpoker.domain.NumberPokerGame;
import coffeeshout.numberpoker.domain.NumberPokerProbabilityAdjuster;
import coffeeshout.numberpoker.domain.PokerRoundResult;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.roulette.Probability;
import coffeeshout.room.domain.service.RoomQueryService;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NumberPokerFlowOrchestrator {

    private static final int MIN_PROBABILITY = 100;

    private final NumberPokerFlowScheduler flowScheduler;
    private final NumberPokerTimingProperties timing;
    private final NumberPokerNotifier notifier;
    private final NumberPokerProbabilityAdjuster probabilityAdjuster;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomQueryService roomQueryService;
    private final DeckShuffler deckShuffler;

    private final ConcurrentHashMap<String, EarlyFinishTrigger> earlyReadyTriggers = new ConcurrentHashMap<>();

    public void startFlow(NumberPokerGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        final int totalRounds = game.getTotalRounds();

        FlowHandle flow = flowScheduler.schedule(startRound(game, room), Duration.ZERO);

        for (int i = 0; i < totalRounds; i++) {
            final boolean isFirst = (i == 0);
            final boolean isLast = (i == totalRounds - 1);
            final EarlyFinishTrigger allFoldedTrigger = flowScheduler.createEarlyFinishTrigger();
            final EarlyFinishTrigger readyTrigger = flowScheduler.createEarlyFinishTrigger();
            flow = chainRound(flow, game, room, allFoldedTrigger, readyTrigger, isFirst, isLast);
        }

        flow.andThen(finishGame(game, room), timing.scoreBoard())
                .onError(ex -> {
                    earlyReadyTriggers.remove(joinCode);
                    log.error("NumberPoker flow 실패: joinCode={}", joinCode, ex);
                });
    }

    public void triggerEarlyRoundReady(String joinCode) {
        final EarlyFinishTrigger trigger = earlyReadyTriggers.get(joinCode);
        if (trigger == null || trigger.isCompleted()) {
            return;
        }
        log.info("전원 레디 완료 - 조기 라운드 전환 트리거: joinCode={}", joinCode);
        trigger.complete();
    }

    // ── Round chaining ────────────────────────────────────────────────────────

    private FlowHandle chainRound(
            FlowHandle flow,
            NumberPokerGame game,
            Room room,
            EarlyFinishTrigger allFoldedTrigger,
            EarlyFinishTrigger readyTrigger,
            boolean isFirst,
            boolean isLast
    ) {
        flow = chainStages(flow, game, room, allFoldedTrigger, isFirst);
        flow = chainShowdownAndScoreBoard(flow, game, room);
        if (isLast) {
            return flow;
        }
        return chainRoundReady(flow, game, room, readyTrigger);
    }

    private FlowHandle chainStages(FlowHandle flow, NumberPokerGame game, Room room,
                                    EarlyFinishTrigger allFoldedTrigger, boolean isFirst) {
        final Duration loadingDuration = isFirst ? timing.firstLoading() : timing.loading();
        flow = flow.andThen(step(game, room, NumberPokerGame::beginStage1), loadingDuration);
        flow = flow.andThen(beginStage2OrSkipOnAllFolded(game, room, allFoldedTrigger), timing.stage1());
        return flow.raceTimeout(timing.stage2(), allFoldedTrigger, Duration.ZERO);
    }

    private FlowHandle chainShowdownAndScoreBoard(FlowHandle flow, NumberPokerGame game, Room room) {
        flow = flow.andThen(step(game, room, NumberPokerGame::showdown), Duration.ZERO);
        return flow.andThen(scoreBoard(game, room), timing.showdown());
    }

    private FlowHandle chainRoundReady(FlowHandle flow, NumberPokerGame game, Room room,
                                        EarlyFinishTrigger readyTrigger) {
        final String joinCode = room.getJoinCode().getValue();
        flow = flow.andThen(() -> {
            earlyReadyTriggers.put(joinCode, readyTrigger);
            step(game, room, NumberPokerGame::beginRoundReady).run();
        }, timing.scoreBoard());
        flow = flow.raceTimeout(timing.roundReady(), readyTrigger, Duration.ZERO);
        return flow.andThen(startRound(game, room), Duration.ZERO);
    }

    // ── Runnable factories ────────────────────────────────────────────────────

    private Runnable startRound(NumberPokerGame game, Room room) {
        return () -> {
            game.startRound(deckShuffler);
            try {
                notifier.notifyPhaseChanged(game, room);
                notifier.notifyHands(game, room);
            } catch (Exception e) {
                log.warn("라운드 시작 알림 실패: joinCode={}", room.getJoinCode().getValue(), e);
            }
        };
    }

    private Runnable finishGame(NumberPokerGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        return () -> {
            earlyReadyTriggers.remove(joinCode);
            final Room freshRoom = roomQueryService.getByJoinCode(new JoinCode(joinCode));
            game.done();
            freshRoom.applyMiniGameResult(game);
            try {
                notifier.notifyPhaseChanged(game, freshRoom);
            } catch (Exception e) {
                log.warn("페이즈 변경 알림 실패: joinCode={}, phase={}", joinCode, game.getCurrentPhase(), e);
            }
            eventPublisher.publishEvent(new MiniGameFinishedEvent(joinCode, MiniGameType.NUMBER_POKER.name()));
            log.info("넘버포커 게임 완료: joinCode={}", joinCode);
        };
    }

    private Runnable beginStage2OrSkipOnAllFolded(NumberPokerGame game, Room room,
                                                   EarlyFinishTrigger allFoldedTrigger) {
        return () -> {
            if (game.isAllFolded()) {
                allFoldedTrigger.complete();
            } else {
                step(game, room, NumberPokerGame::beginStage2).run();
            }
        };
    }

    private Runnable scoreBoard(NumberPokerGame game, Room room) {
        return () -> {
            final Map<Player, Integer> deltas = applyRoundProbabilities(game, room);
            game.scoreBoard();
            try {
                notifier.notifyPhaseChanged(game, room, deltas);
            } catch (Exception e) {
                log.warn("페이즈 변경 알림 실패: joinCode={}, phase={}",
                        room.getJoinCode().getValue(), game.getCurrentPhase(), e);
            }
        };
    }

    private Runnable step(NumberPokerGame game, Room room, java.util.function.Consumer<NumberPokerGame> action) {
        return () -> {
            action.accept(game);
            try {
                notifier.notifyPhaseChanged(game, room);
            } catch (Exception e) {
                log.warn("페이즈 변경 알림 실패: joinCode={}, phase={}",
                        room.getJoinCode().getValue(), game.getCurrentPhase(), e);
            }
        };
    }

    // ── Probability ───────────────────────────────────────────────────────────

    private Map<Player, Integer> applyRoundProbabilities(NumberPokerGame game, Room room) {
        final Map<Player, PokerRoundResult> results = game.getCurrentRoundResults();
        final Map<Player, HandRanking> handRankings = game.getActivePlayerHandRankings();

        final Map<Player, Integer> deltas = probabilityAdjuster.calculate(
                results,
                handRankings,
                room.getPlayers().size(),
                game.getTotalRounds(),
                game.getEffectiveRoundNumber()
        );

        final boolean anyChange = deltas.values().stream().anyMatch(d -> d != 0);
        if (anyChange) {
            game.resetSkippedRounds();
        } else {
            game.addSkippedRound();
        }

        for (Player player : room.getPlayers()) {
            final int delta = deltas.getOrDefault(player, 0);
            final int current = player.getProbability() != null ? player.getProbability().value() : 0;
            final int newValue = Math.max(MIN_PROBABILITY, current + delta);
            player.updateProbability(new Probability(newValue));
        }
        return deltas;
    }
}
