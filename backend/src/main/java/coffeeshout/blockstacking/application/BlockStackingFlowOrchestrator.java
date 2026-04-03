package coffeeshout.blockstacking.application;

import static coffeeshout.blockstacking.domain.BlockStackingGameStep.FINISH_GAME;
import static coffeeshout.blockstacking.domain.BlockStackingGameStep.PREPARE;
import static coffeeshout.blockstacking.domain.BlockStackingGameStep.START_PLAY;

import coffeeshout.blockstacking.config.BlockStackingTimingProperties;
import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.blockstacking.domain.BlockStackingGameStep;
import coffeeshout.global.flow.FlowScheduler;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.domain.Room;
import java.time.Duration;
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
    private final ApplicationEventPublisher eventPublisher;

    public void startFlow(BlockStackingGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();

        blockStackingFlowScheduler.schedule(step(game, room, PREPARE), Duration.ZERO)
                .andThen(step(game, room, START_PLAY), timing.prepare())
                .andThen(finishGame(game, room), timing.playing())
                .onError(ex -> log.error("BlockStacking flow 실패: joinCode={}", joinCode, ex));
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

    private Runnable finishGame(BlockStackingGame game, Room room) {
        return () -> {
            final String joinCode = room.getJoinCode().getValue();
            FINISH_GAME.execute(game, room);
            try {
                notifier.notifyStateChanged(game, room);
                notifier.notifyGameComplete(room);
            } catch (Exception e) {
                log.warn("BlockStacking 완료 알림 실패: joinCode={}", joinCode, e);
            }
            eventPublisher.publishEvent(new MiniGameFinishedEvent(joinCode, MiniGameType.BLOCK_STACKING.name()));
        };
    }
}
