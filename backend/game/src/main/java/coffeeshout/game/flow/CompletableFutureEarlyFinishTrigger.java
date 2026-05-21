package coffeeshout.game.flow;

import coffeeshout.gamecommon.flow.EarlyFinishTrigger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

    @Override
    public CompletionStage<Void> asCompletionStage() {
        return future;
    }
}
