package coffeeshout.cardgame.infra.scheduler;

import coffeeshout.cardgame.application.port.CardGameFlowScheduler;
import coffeeshout.cardgame.application.port.EarlyFinishTrigger;
import coffeeshout.cardgame.application.port.FlowHandle;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CompletableFutureFlowScheduler implements CardGameFlowScheduler {

    private final ScheduledExecutorService executor;

    public CompletableFutureFlowScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public FlowHandle schedule(Runnable action, Duration delay) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.schedule(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
        return new CompletableFutureFlowHandle(future, executor);
    }

    @Override
    public EarlyFinishTrigger createEarlyFinishTrigger() {
        return new CompletableFutureEarlyFinishTrigger();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
