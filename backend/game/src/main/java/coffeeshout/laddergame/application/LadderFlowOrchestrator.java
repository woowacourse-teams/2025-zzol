package coffeeshout.laddergame.application;

import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.laddergame.config.LadderTimingProperties;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.room.domain.Room;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LadderFlowOrchestrator {

    private final FlowScheduler ladderFlowScheduler;
    private final LadderTimingProperties timing;
    private final LadderNotifier notifier;
    private final ApplicationEventPublisher eventPublisher;

    public void startFlow(LadderGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();

        ladderFlowScheduler.schedule(description(room), Duration.ZERO)
                .andThen(prepare(game, room), timing.description())
                .andThen(drawing(game, room), timing.prepare())
                .andThen(result(game, room), timing.drawing().plus(timing.drawingGracePeriod()))
                .andThen(done(game, room), timing.result())
                .onError(ex -> log.error("사다리게임 flow 실패: joinCode={}", joinCode, ex));
    }

    private Runnable description(Room room) {
        return () -> {
            try {
                notifier.notifyDescription(room);
            } catch (Exception e) {
                log.warn("사다리게임 DESCRIPTION 알림 실패: joinCode={}", room.getJoinCode().getValue(), e);
            }
        };
    }

    private Runnable prepare(LadderGame game, Room room) {
        return () -> {
            game.changeToPrepare();
            try {
                notifier.notifyPrepare(game, room);
            } catch (Exception e) {
                log.warn("사다리게임 PREPARE 알림 실패: joinCode={}", room.getJoinCode().getValue(), e);
            }
        };
    }

    private Runnable drawing(LadderGame game, Room room) {
        return () -> {
            game.changeToDrawing();
            final Instant drawingEndTime = Instant.now().plus(timing.drawing());
            try {
                notifier.notifyDrawing(room, drawingEndTime);
            } catch (Exception e) {
                log.warn("사다리게임 DRAWING 알림 실패: joinCode={}", room.getJoinCode().getValue(), e);
            }
        };
    }

    private Runnable result(LadderGame game, Room room) {
        return () -> {
            game.changeToResult();
            game.tracePaths();
            try {
                notifier.notifyResult(game, room, timing.result().toMillis());
            } catch (Exception e) {
                log.warn("사다리게임 RESULT 알림 실패: joinCode={}", room.getJoinCode().getValue(), e);
            }
        };
    }

    private Runnable done(LadderGame game, Room room) {
        final String joinCode = room.getJoinCode().getValue();
        return () -> {
            game.changeToDone();
            try {
                notifier.notifyDone(room);
            } catch (Exception e) {
                log.warn("사다리게임 DONE 알림 실패: joinCode={}", joinCode, e);
            }
            eventPublisher.publishEvent(new MiniGameFinishedEvent(joinCode, MiniGameType.LADDER_GAME.name()));
        };
    }
}
