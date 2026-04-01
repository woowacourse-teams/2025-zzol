package coffeeshout.numberpoker.application;

import coffeeshout.cardgame.application.port.EarlyFinishTrigger;
import coffeeshout.cardgame.application.port.FlowHandle;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.numberpoker.application.port.NumberPokerFlowScheduler;
import coffeeshout.numberpoker.config.NumberPokerTimingProperties;
import coffeeshout.numberpoker.domain.NumberPokerGame;
import coffeeshout.numberpoker.domain.NumberPokerProbabilityAdjuster;
import coffeeshout.numberpoker.domain.PokerPhase;
import coffeeshout.numberpoker.domain.PokerRoundResult;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.roulette.Probability;
import coffeeshout.room.domain.service.RoomQueryService;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
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

    private final ConcurrentHashMap<String, EarlyFinishTrigger> earlyReadyTriggers = new ConcurrentHashMap<>();

    private final Random random = new Random();

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

    private FlowHandle chainRound(FlowHandle flow, NumberPokerGame game, Room room,
                                   EarlyFinishTrigger allFoldedTrigger,
                                   EarlyFinishTrigger readyTrigger,
                                   boolean isFirst,
                                   boolean isLast) {
        final Duration loadingDuration = isFirst ? timing.firstLoading() : timing.loading();

        // STAGE_1 (LOADING 표시 후)
        flow = flow.andThen(step(game, room, NumberPokerGame::beginStage1), loadingDuration);

        // STAGE_1 종료 후: 전원 폴드 여부에 따라 STAGE_2 진입 또는 스킵
        flow = flow.andThen(() -> {
            if (game.isAllFolded()) {
                allFoldedTrigger.complete();
            } else {
                step(game, room, NumberPokerGame::beginStage2).run();
            }
        }, timing.stage1());

        // STAGE_2 대기 (전원 폴드 시 즉시 통과)
        flow = flow.raceTimeout(timing.stage2(), allFoldedTrigger, Duration.ZERO);

        // SHOWDOWN
        flow = flow.andThen(step(game, room, NumberPokerGame::showdown), Duration.ZERO);

        // SCORE_BOARD (SHOWDOWN 표시 후, 확률 적용 포함)
        flow = flow.andThen(() -> {
            final Map<Player, Integer> deltas = applyRoundProbabilities(game, room);
            game.scoreBoard();
            try {
                notifier.notifyPhaseChanged(game, room, deltas);
            } catch (Exception e) {
                log.warn("페이즈 변경 알림 실패: joinCode={}, phase={}",
                        room.getJoinCode().getValue(), game.getCurrentPhase(), e);
            }
        }, timing.showdown());

        if (isLast) {
            return flow;
        }

        // ROUND_READY (SCORE_BOARD 표시 후)
        final String joinCode = room.getJoinCode().getValue();
        flow = flow.andThen(() -> {
            earlyReadyTriggers.put(joinCode, readyTrigger);
            step(game, room, NumberPokerGame::beginRoundReady).run();
        }, timing.scoreBoard());

        // 전원 레디 또는 타임아웃 대기
        flow = flow.raceTimeout(timing.roundReady(), readyTrigger, Duration.ZERO);

        // 다음 라운드 시작
        flow = flow.andThen(startRound(game, room), Duration.ZERO);

        return flow;
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

    private Runnable startRound(NumberPokerGame game, Room room) {
        return () -> {
            game.startRound(random);
            try {
                notifier.notifyPhaseChanged(game, room);
                notifier.notifyHands(game, room);
            } catch (Exception e) {
                log.warn("라운드 시작 알림 실패: joinCode={}", room.getJoinCode().getValue(), e);
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

    private Map<Player, Integer> applyRoundProbabilities(NumberPokerGame game, Room room) {
        final Map<Player, PokerRoundResult> results = game.getCurrentRoundResults();
        final Map<Player, Integer> deltas = probabilityAdjuster.calculate(
                results,
                room.getPlayers().size(),
                game.getTotalRounds()
        );
        for (Player player : room.getPlayers()) {
            final int delta = deltas.getOrDefault(player, 0);
            final int current = player.getProbability() != null ? player.getProbability().value() : 0;
            final int newValue = Math.max(MIN_PROBABILITY, current + delta);
            player.updateProbability(new Probability(newValue));
        }
        return deltas;
    }
}
