package coffeeshout.cardgame.infra.scheduler;

import coffeeshout.cardgame.application.port.EarlyFinishTrigger;
import coffeeshout.cardgame.application.port.FlowHandle;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CompletableFutureFlowHandle implements FlowHandle {

    private final CompletableFuture<Void> future;
    private final ScheduledExecutorService executor;

    public CompletableFutureFlowHandle(CompletableFuture<Void> future, ScheduledExecutorService executor) {
        this.future = future;
        this.executor = executor;
    }

    @Override
    public FlowHandle andThen(Runnable action, Duration delay) {
        CompletableFuture<Void> next = future.thenCompose(v -> {
            CompletableFuture<Void> step = new CompletableFuture<>();
            executor.schedule(() -> {
                try {
                    action.run();
                    step.complete(null);
                } catch (Exception e) {
                    step.completeExceptionally(e);
                }
            }, delay.toMillis(), TimeUnit.MILLISECONDS);
            return step;
        });
        return new CompletableFutureFlowHandle(next, executor);
    }

    @Override
    public FlowHandle raceTimeout(Duration timeout, EarlyFinishTrigger trigger, Duration earlyFinishExtraDelay) {
        CompletableFuture<Void> triggerFuture = trigger.asCompletionStage().toCompletableFuture();

        CompletableFuture<Void> raced = future.thenCompose(v -> {
            CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
            executor.schedule(() -> timeoutFuture.complete(null), timeout.toMillis(), TimeUnit.MILLISECONDS);

            return CompletableFuture.anyOf(timeoutFuture, triggerFuture)
                    .thenCompose(winner -> {
                        // trigger가 이겼고 timeout은 아직 완료되지 않은 경우만 추가 대기
                        if (triggerFuture.isDone() && !timeoutFuture.isDone()) {
                            CompletableFuture<Void> extraDelay = new CompletableFuture<>();
                            executor.schedule(
                                    () -> extraDelay.complete(null),
                                    earlyFinishExtraDelay.toMillis(),
                                    TimeUnit.MILLISECONDS
                            );
                            // timeout 잔여 시간이 extraDelay보다 짧으면 timeout이 먼저 완료되어 즉시 진행
                            return CompletableFuture.anyOf(extraDelay, timeoutFuture).thenApply(ignored -> null);
                        }
                        return CompletableFuture.completedFuture(null);
                    });
        });
        return new CompletableFutureFlowHandle(raced, executor);
    }

    @Override
    public FlowHandle onError(Consumer<Throwable> errorHandler) {
        CompletableFuture<Void> recovered = future.exceptionally(ex -> {
            errorHandler.accept(ex);
            return null;
        });
        return new CompletableFutureFlowHandle(recovered, executor);
    }
}
