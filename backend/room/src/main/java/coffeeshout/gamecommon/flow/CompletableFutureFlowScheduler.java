package coffeeshout.gamecommon.flow;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;

@RequiredArgsConstructor
public class CompletableFutureFlowScheduler implements FlowScheduler {

    private final TaskScheduler taskScheduler;

    @Override
    public FlowHandle schedule(Runnable action, Duration delay) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        taskScheduler.schedule(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, Instant.now().plus(delay));
        return new CompletableFutureFlowHandle(future, taskScheduler);
    }

    @Override
    public EarlyFinishTrigger createEarlyFinishTrigger() {
        return new CompletableFutureEarlyFinishTrigger();
    }
}
