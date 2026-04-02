package coffeeshout.cardgame.infra.scheduler;

import coffeeshout.global.flow.EarlyFinishTrigger;
import coffeeshout.global.flow.FlowHandle;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;

@RequiredArgsConstructor
public class CompletableFutureFlowHandle implements FlowHandle {

    private final CompletableFuture<Void> future;
    private final TaskScheduler taskScheduler;

    @Override
    public FlowHandle andThen(Runnable action, Duration delay) {
        CompletableFuture<Void> next = future.thenCompose(v -> {
            CompletableFuture<Void> step = new CompletableFuture<>();
            taskScheduler.schedule(() -> {
                try {
                    action.run();
                    step.complete(null);
                } catch (Exception e) {
                    step.completeExceptionally(e);
                }
            }, Instant.now().plus(delay));
            return step;
        });
        return new CompletableFutureFlowHandle(next, taskScheduler);
    }

    @Override
    public FlowHandle raceTimeout(Duration timeout, EarlyFinishTrigger trigger, Duration earlyFinishExtraDelay) {
        CompletableFuture<Void> triggerFuture = trigger.asCompletionStage().toCompletableFuture();

        CompletableFuture<Void> raced = future.thenCompose(v -> {
            CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
            taskScheduler.schedule(() -> timeoutFuture.complete(null), Instant.now().plus(timeout));

            return CompletableFuture.anyOf(timeoutFuture, triggerFuture)
                    .thenCompose(winner -> {
                        // trigger가 이겼고 timeout은 아직 완료되지 않은 경우만 추가 대기
                        if (triggerFuture.isDone() && !timeoutFuture.isDone()) {
                            CompletableFuture<Void> extraDelay = new CompletableFuture<>();
                            taskScheduler.schedule(
                                    () -> extraDelay.complete(null),
                                    Instant.now().plus(earlyFinishExtraDelay)
                            );
                            // timeout 잔여 시간이 extraDelay보다 짧으면 timeout이 먼저 완료되어 즉시 진행
                            return CompletableFuture.anyOf(extraDelay, timeoutFuture).thenApply(ignored -> null);
                        }
                        return CompletableFuture.completedFuture(null);
                    });
        });
        return new CompletableFutureFlowHandle(raced, taskScheduler);
    }

    @Override
    public FlowHandle onError(Consumer<Throwable> errorHandler) {
        CompletableFuture<Void> recovered = future.exceptionally(ex -> {
            errorHandler.accept(ex);
            return null;
        });
        return new CompletableFutureFlowHandle(recovered, taskScheduler);
    }
}
