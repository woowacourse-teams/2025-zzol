package coffeeshout.cardgame.application;

import static coffeeshout.cardgame.domain.CardGameStep.FINISH_GAME;
import static coffeeshout.cardgame.domain.CardGameStep.FINISH_ROUND;
import static coffeeshout.cardgame.domain.CardGameStep.PREPARE;
import static coffeeshout.cardgame.domain.CardGameStep.START_PLAY;
import static coffeeshout.cardgame.domain.CardGameStep.START_ROUND;

import coffeeshout.cardgame.application.port.CardGameFlowScheduler;
import coffeeshout.cardgame.application.port.EarlyFinishTrigger;
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

        EarlyFinishTrigger round1Trigger = flowScheduler.createEarlyFinishTrigger();
        EarlyFinishTrigger round2Trigger = flowScheduler.createEarlyFinishTrigger();

        flowScheduler.schedule(step(cardGame, room, START_ROUND), Duration.ZERO)

                // 1라운드: 로딩 → 설명 → 플레이
                .andThen(step(cardGame, room, PREPARE), timing.firstLoading())
                .andThen(() -> {
                    earlyFinishTriggers.put(joinCode, round1Trigger);
                    step(cardGame, room, START_PLAY).run();
                }, timing.prepare())
                .raceTimeout(timing.playing(), round1Trigger, timing.earlyFinishDelay())

                // 1라운드 종료 → 2라운드: 로딩 → 플레이
                .andThen(step(cardGame, room, FINISH_ROUND), Duration.ZERO)
                .andThen(step(cardGame, room, START_ROUND), timing.scoreBoard())
                .andThen(() -> {
                    earlyFinishTriggers.put(joinCode, round2Trigger);
                    step(cardGame, room, START_PLAY).run();
                }, timing.loading())
                .raceTimeout(timing.playing(), round2Trigger, timing.earlyFinishDelay())

                // 2라운드 종료 → 게임 종료
                .andThen(step(cardGame, room, FINISH_ROUND), Duration.ZERO)
                .andThen(() -> {
                    earlyFinishTriggers.remove(joinCode);
                    step(cardGame, room, FINISH_GAME).run();
                    eventPublisher.publishEvent(new MiniGameFinishedEvent(joinCode, MiniGameType.CARD_GAME.name()));
                }, timing.scoreBoard())

                .onError(ex -> log.error("CardGame flow 실패: joinCode={}", joinCode, ex));
    }

    public void triggerEarlyRoundFinish(String joinCode) {
        EarlyFinishTrigger trigger = earlyFinishTriggers.get(joinCode);
        if (trigger != null && !trigger.isCompleted()) {
            log.info("전원 카드 선택 완료 - 조기 라운드 종료 트리거: joinCode={}", joinCode);
            trigger.complete();
        }
    }

    private Runnable step(CardGame cardGame, Room room, CardGameStep step) {
        return () -> {
            step.execute(cardGame, room);
            notifier.notifyStepCompleted(cardGame, room);
        };
    }
}
