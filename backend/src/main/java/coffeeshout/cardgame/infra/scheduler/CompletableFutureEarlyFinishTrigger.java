package coffeeshout.cardgame.infra.scheduler;

import coffeeshout.cardgame.application.port.EarlyFinishTrigger;
import java.util.concurrent.CompletableFuture;

public class CompletableFutureEarlyFinishTrigger implements EarlyFinishTrigger {

    private final CompletableFuture<Void> future;

    public CompletableFutureEarlyFinishTrigger() {
        this.future = new CompletableFuture<>();
    }

    @Override
    public void complete() {
        future.complete(null);
    }

    @Override
    public boolean isCompleted() {
        return future.isDone();
    }

    CompletableFuture<Void> getFuture() {
        return future;
    }
}
