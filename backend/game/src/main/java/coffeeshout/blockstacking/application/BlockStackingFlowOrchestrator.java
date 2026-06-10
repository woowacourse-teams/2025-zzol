package coffeeshout.blockstacking.application;

import static coffeeshout.blockstacking.domain.BlockStackingGameStep.FINISH_GAME;
import static coffeeshout.blockstacking.domain.BlockStackingGameStep.PREPARE;
import static coffeeshout.blockstacking.domain.BlockStackingGameStep.START_PLAY;

import coffeeshout.blockstacking.config.BlockStackingTimingProperties;
import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.blockstacking.domain.BlockStackingGameStep;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.flow.EarlyFinishTrigger;
import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockStackingFlowOrchestrator {

    private final FlowScheduler blockStackingFlowScheduler;
    private final BlockStackingTimingProperties timing;
    private final BlockStackingNotifier notifier;
    private final GameSessionService gameSessionService;
    private final ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, EarlyFinishTrigger> earlyFinishTriggers = new ConcurrentHashMap<>();

    public void startFlow(BlockStackingGame game, JoinCode joinCode) {
        final String joinCodeValue = joinCode.getValue();
        final EarlyFinishTrigger trigger = blockStackingFlowScheduler.createEarlyFinishTrigger();

        blockStackingFlowScheduler.schedule(step(game, joinCode, PREPARE), Duration.ZERO)
                .andThen(startPlay(game, joinCode, trigger), timing.prepare())
                .raceTimeout(timing.playing(), trigger, timing.allFailedDelay())
                .andThen(finishGame(game, joinCode), Duration.ZERO)
                .onError(ex -> {
                    earlyFinishTriggers.remove(joinCodeValue);
                    log.error("BlockStacking flow 실패: joinCode={}", joinCodeValue, ex);
                });
    }

    public void triggerEarlyFinishIfAllFailed(String joinCode, BlockStackingGame game) {
        if (!game.isAllPlayersFailed()) {
            return;
        }
        final EarlyFinishTrigger trigger = earlyFinishTriggers.get(joinCode);
        if (trigger == null || trigger.isCompleted()) {
            return;
        }
        log.info("전원 실패 — 조기 종료 트리거: joinCode={}", joinCode);
        trigger.complete();
    }

    private Runnable step(BlockStackingGame game, JoinCode joinCode, BlockStackingGameStep gameStep) {
        return () -> {
            gameStep.execute(game);
            try {
                notifier.notifyStateChanged(game, joinCode);
            } catch (Exception e) {
                log.warn("BlockStacking step 알림 실패: joinCode={}, step={}",
                        joinCode.getValue(), gameStep, e);
            }
        };
    }

    private Runnable startPlay(BlockStackingGame game, JoinCode joinCode, EarlyFinishTrigger trigger) {
        return () -> {
            earlyFinishTriggers.put(joinCode.getValue(), trigger);
            START_PLAY.execute(game);
            final Instant playingEndTime = Instant.now().plus(timing.playing());
            try {
                notifier.notifyPlayingStarted(joinCode, playingEndTime);
            } catch (Exception e) {
                log.warn("BlockStacking PLAYING 알림 실패: joinCode={}", joinCode.getValue(), e);
            }
        };
    }

    private Runnable finishGame(BlockStackingGame game, JoinCode joinCode) {
        final String joinCodeValue = joinCode.getValue();
        return () -> {
            earlyFinishTriggers.remove(joinCodeValue);
            FINISH_GAME.execute(game);
            try {
                notifier.notifyStateChanged(game, joinCode);
            } catch (Exception e) {
                log.warn("BlockStacking 완료 알림 실패: joinCode={}", joinCodeValue, e);
            }
            // 순서 불변식(ADR-0023 결정 5): finishGame()으로 roundCount 확정·상태 복귀 후 이벤트 발행
            final int roundCount = gameSessionService.finishGame(joinCode);
            eventPublisher.publishEvent(new MiniGameFinishedEvent(
                    joinCodeValue, MiniGameType.BLOCK_STACKING.name(), game.getResult().toRankMap(), roundCount));
        };
    }
}
