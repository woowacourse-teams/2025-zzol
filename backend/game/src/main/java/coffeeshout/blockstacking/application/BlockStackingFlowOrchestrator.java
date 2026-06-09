package coffeeshout.blockstacking.application;

import static coffeeshout.blockstacking.domain.BlockStackingGameStep.FINISH_GAME;
import static coffeeshout.blockstacking.domain.BlockStackingGameStep.PREPARE;
import static coffeeshout.blockstacking.domain.BlockStackingGameStep.START_PLAY;

import coffeeshout.blockstacking.config.BlockStackingTimingProperties;
import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.blockstacking.domain.BlockStackingGameStep;
import coffeeshout.gamecommon.flow.EarlyFinishTrigger;
import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.domain.Room;
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

    public void startFlow(BlockStackingGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        final EarlyFinishTrigger trigger = blockStackingFlowScheduler.createEarlyFinishTrigger();

        blockStackingFlowScheduler.schedule(step(game, room, PREPARE), Duration.ZERO)
                .andThen(startPlay(game, room, trigger), timing.prepare())
                .raceTimeout(timing.playing(), trigger, timing.allFailedDelay())
                .andThen(finishGame(game, room), Duration.ZERO)
                .onError(ex -> {
                    earlyFinishTriggers.remove(joinCode);
                    log.error("BlockStacking flow 실패: joinCode={}", joinCode, ex);
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

    private Runnable step(BlockStackingGame game, Room room, BlockStackingGameStep gameStep) {
        return () -> {
            gameStep.execute(game, room);
            try {
                notifier.notifyStateChanged(game, room);
            } catch (Exception e) {
                log.warn("BlockStacking step 알림 실패: joinCode={}, step={}",
                        room.getJoinCode().getValue(), gameStep, e);
            }
        };
    }

    private Runnable startPlay(BlockStackingGame game, Room room, EarlyFinishTrigger trigger) {
        return () -> {
            final String joinCode = room.getJoinCode().getValue();
            earlyFinishTriggers.put(joinCode, trigger);
            START_PLAY.execute(game, room);
            final Instant playingEndTime = Instant.now().plus(timing.playing());
            try {
                notifier.notifyPlayingStarted(room, playingEndTime);
            } catch (Exception e) {
                log.warn("BlockStacking PLAYING 알림 실패: joinCode={}", joinCode, e);
            }
        };
    }

    private Runnable finishGame(BlockStackingGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        return () -> {
            earlyFinishTriggers.remove(joinCode);
            FINISH_GAME.execute(game, room);
            try {
                notifier.notifyStateChanged(game, room);
            } catch (Exception e) {
                log.warn("BlockStacking 완료 알림 실패: joinCode={}", joinCode, e);
            }
            // 순서 불변식(ADR-0023 결정 5): finishGame()으로 roundCount 확정·상태 복귀 후 이벤트 발행
            final int roundCount = gameSessionService.finishGame(room.getJoinCode());
            eventPublisher.publishEvent(new MiniGameFinishedEvent(
                    joinCode, MiniGameType.BLOCK_STACKING.name(), game.getResult().toRankMap(), roundCount));
        };
    }
}
