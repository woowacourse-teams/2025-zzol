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

        FlowHandle flow = flowScheduler.schedule(step(cardGame, room, START_ROUND), Duration.ZERO);

        for (int currentRound = 0; currentRound < timing.totalRounds(); currentRound++) {
            EarlyFinishTrigger trigger = flowScheduler.createEarlyFinishTrigger();
            flow = isFirstRound(currentRound)
                    ? chainFirstRound(flow, cardGame, room, trigger)
                    : chainSubsequentRound(flow, cardGame, room, trigger);
            flow = finishRound(flow, cardGame, room, currentRound);
        }

        flow.andThen(finishGame(cardGame, room), timing.scoreBoard())
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

    private boolean isFirstRound(int roundIndex) {
        return roundIndex == 0;
    }

    private boolean isLastRound(int roundIndex) {
        return roundIndex == timing.totalRounds() - 1;
    }

    private FlowHandle chainFirstRound(FlowHandle flow, CardGame cardGame, Room room,
                                       EarlyFinishTrigger trigger) {
        return flow
                .andThen(step(cardGame, room, PREPARE), timing.firstLoading())
                .andThen(startPlay(cardGame, room, trigger), timing.prepare())
                .raceTimeout(timing.playing(), trigger, timing.earlyFinishDelay());
    }

    private FlowHandle chainSubsequentRound(FlowHandle flow, CardGame cardGame, Room room,
                                            EarlyFinishTrigger trigger) {
        return flow
                .andThen(startPlay(cardGame, room, trigger), timing.loading())
                .raceTimeout(timing.playing(), trigger, timing.earlyFinishDelay());
    }

    private FlowHandle finishRound(FlowHandle flow, CardGame cardGame, Room room, int roundIndex) {
        flow = flow.andThen(step(cardGame, room, FINISH_ROUND), Duration.ZERO);
        if (isLastRound(roundIndex)) {
            return flow;
        }
        return flow.andThen(step(cardGame, room, START_ROUND), timing.scoreBoard());
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
            notifier.notifyStepCompleted(cardGame, room);
        };
    }
}
