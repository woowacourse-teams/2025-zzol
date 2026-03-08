package coffeeshout.cardgame.application;

import static coffeeshout.cardgame.domain.CardGameStep.FINISH_GAME;
import static coffeeshout.cardgame.domain.CardGameStep.FINISH_ROUND;
import static coffeeshout.cardgame.domain.CardGameStep.PREPARE;
import static coffeeshout.cardgame.domain.CardGameStep.START_PLAY;
import static coffeeshout.cardgame.domain.CardGameStep.START_ROUND;

import coffeeshout.cardgame.application.port.CardGameFlowScheduler;
import coffeeshout.cardgame.application.port.EarlyFinishTrigger;
import coffeeshout.cardgame.application.port.FlowHandle;
import coffeeshout.cardgame.config.CardGameTimingProperties;
import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.CardGameState;
import coffeeshout.cardgame.domain.CardGameStep;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.domain.Room;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardGameFlowOrchestrator {

    private final CardGameFlowScheduler flowScheduler;
    private final CardGameTimingProperties timing;
    private final CardGameNotifier notifier;
    private final ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, EarlyFinishTrigger> earlyFinishTriggers = new ConcurrentHashMap<>();

    public void startFlow(CardGame cardGame, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        final int totalRounds = cardGame.getTotalRounds();

        FlowHandle flow = flowScheduler.schedule(step(cardGame, room, START_ROUND), Duration.ZERO);

        for (int i = 0; i < totalRounds; i++) {
            EarlyFinishTrigger trigger = flowScheduler.createEarlyFinishTrigger();
            flow = (i == 0)
                    ? chainFirstRound(flow, cardGame, room, trigger)
                    : chainSubsequentRound(flow, cardGame, room, trigger);
            flow = finishRound(flow, cardGame, room, i == totalRounds - 1);
        }

        flow.andThen(finishGame(cardGame, room), durationOf(CardGameState.SCORE_BOARD))
                .onError(ex -> {
                    earlyFinishTriggers.remove(joinCode);
                    log.error("CardGame flow 실패: joinCode={}", joinCode, ex);
                });
    }

    public void triggerEarlyRoundFinish(String joinCode) {
        EarlyFinishTrigger trigger = earlyFinishTriggers.get(joinCode);
        if (trigger == null || trigger.isCompleted()) {
            return;
        }
        log.info("전원 카드 선택 완료 - 조기 라운드 종료 트리거: joinCode={}", joinCode);
        trigger.complete();
    }

    private FlowHandle chainFirstRound(FlowHandle flow, CardGame cardGame, Room room,
                                       EarlyFinishTrigger trigger) {
        return flow
                .andThen(step(cardGame, room, PREPARE), durationOf(CardGameState.FIRST_LOADING))
                .andThen(startPlay(cardGame, room, trigger), durationOf(CardGameState.PREPARE))
                .raceTimeout(durationOf(CardGameState.PLAYING), trigger, timing.earlyFinishDelay());
    }

    private FlowHandle chainSubsequentRound(FlowHandle flow, CardGame cardGame, Room room,
                                            EarlyFinishTrigger trigger) {
        return flow
                .andThen(startPlay(cardGame, room, trigger), durationOf(CardGameState.LOADING))
                .raceTimeout(durationOf(CardGameState.PLAYING), trigger, timing.earlyFinishDelay());
    }

    private FlowHandle finishRound(FlowHandle flow, CardGame cardGame, Room room, boolean isLastRound) {
        flow = flow.andThen(step(cardGame, room, FINISH_ROUND), Duration.ZERO);
        if (isLastRound) {
            return flow;
        }
        return flow.andThen(step(cardGame, room, START_ROUND), durationOf(CardGameState.SCORE_BOARD));
    }

    private Duration durationOf(CardGameState state) {
        return switch (state) {
            case FIRST_LOADING -> timing.firstLoading();
            case LOADING -> timing.loading();
            case PREPARE -> timing.prepare();
            case PLAYING -> timing.playing();
            case SCORE_BOARD -> timing.scoreBoard();
            case READY, DONE -> throw new IllegalArgumentException(
                    "타이밍이 정의되지 않은 상태입니다: " + state);
        };
    }

    private Runnable startPlay(CardGame cardGame, Room room, EarlyFinishTrigger trigger) {
        return () -> {
            earlyFinishTriggers.put(room.getJoinCode().getValue(), trigger);
            step(cardGame, room, START_PLAY).run();
        };
    }

    private Runnable finishGame(CardGame cardGame, Room room) {
        String joinCode = room.getJoinCode().getValue();
        return () -> {
            earlyFinishTriggers.remove(joinCode);
            step(cardGame, room, FINISH_GAME).run();
            eventPublisher.publishEvent(new MiniGameFinishedEvent(joinCode, MiniGameType.CARD_GAME.name()));
        };
    }

    private Runnable step(CardGame cardGame, Room room, CardGameStep step) {
        return () -> {
            step.execute(cardGame, room);
            try {
                notifier.notifyStepCompleted(cardGame, room);
            } catch (Exception e) {
                log.warn("CardGame step 알림 실패: joinCode={}, step={}", room.getJoinCode().getValue(), step, e);
            }
        };
    }
}
