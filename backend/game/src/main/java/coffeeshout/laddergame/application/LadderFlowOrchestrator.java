package coffeeshout.laddergame.application;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.laddergame.config.LadderTimingProperties;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
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
    private final GameSessionService gameSessionService;
    private final ApplicationEventPublisher eventPublisher;

    public void startFlow(LadderGame game, JoinCode joinCode) {
        ladderFlowScheduler.schedule(description(joinCode), Duration.ZERO)
                .andThen(prepare(game, joinCode), timing.description())
                .andThen(drawing(game, joinCode), timing.prepare())
                .andThen(result(game, joinCode), timing.drawing().plus(timing.drawingGracePeriod()))
                .andThen(done(game, joinCode), timing.result())
                .onError(ex -> log.error("사다리게임 flow 실패: joinCode={}", joinCode.getValue(), ex));
    }

    private Runnable description(JoinCode joinCode) {
        return () -> {
            try {
                notifier.notifyDescription(joinCode);
            } catch (Exception e) {
                log.warn("사다리게임 DESCRIPTION 알림 실패: joinCode={}", joinCode.getValue(), e);
            }
        };
    }

    private Runnable prepare(LadderGame game, JoinCode joinCode) {
        return () -> {
            game.changeToPrepare();
            try {
                notifier.notifyPrepare(game, joinCode);
            } catch (Exception e) {
                log.warn("사다리게임 PREPARE 알림 실패: joinCode={}", joinCode.getValue(), e);
            }
        };
    }

    private Runnable drawing(LadderGame game, JoinCode joinCode) {
        return () -> {
            game.changeToDrawing();
            final Instant drawingEndTime = Instant.now().plus(timing.drawing());
            try {
                notifier.notifyDrawing(joinCode, drawingEndTime);
            } catch (Exception e) {
                log.warn("사다리게임 DRAWING 알림 실패: joinCode={}", joinCode.getValue(), e);
            }
        };
    }

    private Runnable result(LadderGame game, JoinCode joinCode) {
        return () -> {
            game.changeToResult();
            game.tracePaths();
            try {
                notifier.notifyResult(game, joinCode, timing.result().toMillis());
            } catch (Exception e) {
                log.warn("사다리게임 RESULT 알림 실패: joinCode={}", joinCode.getValue(), e);
            }
        };
    }

    private Runnable done(LadderGame game, JoinCode joinCode) {
        final String joinCodeValue = joinCode.getValue();
        return () -> {
            game.changeToDone();
            try {
                notifier.notifyDone(joinCode);
            } catch (Exception e) {
                log.warn("사다리게임 DONE 알림 실패: joinCode={}", joinCodeValue, e);
            }
            // 순서 불변식(ADR-0023 결정 5): finishGame()으로 roundCount 확정·상태 복귀 후 이벤트 발행
            final int roundCount = gameSessionService.finishGame(joinCode);
            eventPublisher.publishEvent(new MiniGameFinishedEvent(
                    joinCodeValue, MiniGameType.LADDER_GAME.name(), game.getResult().toRankMap(), roundCount));
        };
    }
}
