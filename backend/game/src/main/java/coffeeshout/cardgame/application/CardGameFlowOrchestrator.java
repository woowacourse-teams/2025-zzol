package coffeeshout.cardgame.application;

import static coffeeshout.cardgame.domain.CardGameStep.FINISH_GAME;
import static coffeeshout.cardgame.domain.CardGameStep.FINISH_ROUND;
import static coffeeshout.cardgame.domain.CardGameStep.PREPARE;
import static coffeeshout.cardgame.domain.CardGameStep.START_PLAY;
import static coffeeshout.cardgame.domain.CardGameStep.START_ROUND;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.flow.EarlyFinishTrigger;
import coffeeshout.gamecommon.flow.FlowHandle;
import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.cardgame.config.CardGameTimingProperties;
import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.cardgame.domain.CardGameState;
import coffeeshout.cardgame.domain.CardGameStep;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
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

    private final FlowScheduler cardGameFlowScheduler;
    private final CardGameTimingProperties timing;
    private final CardGameNotifier notifier;
    private final GameSessionService gameSessionService;
    private final ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, EarlyFinishTrigger> earlyFinishTriggers = new ConcurrentHashMap<>();

    public void startFlow(CardGame cardGame, JoinCode joinCode) {
        final String joinCodeValue = joinCode.getValue();
        final int totalRounds = cardGame.getTotalRounds();

        FlowHandle flow = cardGameFlowScheduler.schedule(step(cardGame, joinCode, START_ROUND), Duration.ZERO);

        for (int i = 0; i < totalRounds; i++) {
            EarlyFinishTrigger trigger = cardGameFlowScheduler.createEarlyFinishTrigger();
            flow = (i == 0)
                    ? chainFirstRound(flow, cardGame, joinCode, trigger)
                    : chainSubsequentRound(flow, cardGame, joinCode, trigger);
            flow = finishRound(flow, cardGame, joinCode, i == totalRounds - 1);
        }

        flow.andThen(finishGame(cardGame, joinCode), durationOf(CardGameState.SCORE_BOARD))
                .onError(ex -> {
                    earlyFinishTriggers.remove(joinCodeValue);
                    log.error("CardGame flow 실패: joinCode={}", joinCodeValue, ex);
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

    private FlowHandle chainFirstRound(FlowHandle flow, CardGame cardGame, JoinCode joinCode,
                                       EarlyFinishTrigger trigger) {
        return flow
                .andThen(step(cardGame, joinCode, PREPARE), durationOf(CardGameState.FIRST_LOADING))
                .andThen(startPlay(cardGame, joinCode, trigger), durationOf(CardGameState.PREPARE))
                .raceTimeout(durationOf(CardGameState.PLAYING), trigger, timing.earlyFinishDelay());
    }

    private FlowHandle chainSubsequentRound(FlowHandle flow, CardGame cardGame, JoinCode joinCode,
                                            EarlyFinishTrigger trigger) {
        return flow
                .andThen(startPlay(cardGame, joinCode, trigger), durationOf(CardGameState.LOADING))
                .raceTimeout(durationOf(CardGameState.PLAYING), trigger, timing.earlyFinishDelay());
    }

    private FlowHandle finishRound(FlowHandle flow, CardGame cardGame, JoinCode joinCode, boolean isLastRound) {
        flow = flow.andThen(step(cardGame, joinCode, FINISH_ROUND), Duration.ZERO);
        if (isLastRound) {
            return flow;
        }
        return flow.andThen(step(cardGame, joinCode, START_ROUND), durationOf(CardGameState.SCORE_BOARD));
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

    private Runnable startPlay(CardGame cardGame, JoinCode joinCode, EarlyFinishTrigger trigger) {
        return () -> {
            earlyFinishTriggers.put(joinCode.getValue(), trigger);
            step(cardGame, joinCode, START_PLAY).run();
        };
    }

    private Runnable finishGame(CardGame cardGame, JoinCode joinCode) {
        final String joinCodeValue = joinCode.getValue();
        return () -> {
            earlyFinishTriggers.remove(joinCodeValue);
            step(cardGame, joinCode, FINISH_GAME).run();
            // 순서 불변식(ADR-0025 결정 5): finishGame()으로 roundCount 확정·상태 복귀 후 이벤트 발행
            final int roundCount = gameSessionService.finishGame(joinCode);
            eventPublisher.publishEvent(new MiniGameFinishedEvent(
                    joinCodeValue, MiniGameType.CARD_GAME.name(), cardGame.getResult().toRankMap(), roundCount));
        };
    }

    private Runnable step(CardGame cardGame, JoinCode joinCode, CardGameStep step) {
        return () -> {
            step.execute(cardGame);
            try {
                notifier.notifyStepCompleted(cardGame, joinCode);
            } catch (Exception e) {
                log.warn("CardGame step 알림 실패: joinCode={}, step={}", joinCode.getValue(), step, e);
            }
        };
    }
}
