package coffeeshout.numberpoker.infra.scheduler;

import coffeeshout.global.flow.EarlyFinishTrigger;
import coffeeshout.global.flow.FlowHandle;
import coffeeshout.cardgame.infra.scheduler.CompletableFutureFlowScheduler;
import coffeeshout.numberpoker.application.port.NumberPokerFlowScheduler;
import java.time.Duration;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CompletableFutureNumberPokerFlowScheduler implements NumberPokerFlowScheduler {

    private final CompletableFutureFlowScheduler delegate;

    @Override
    public FlowHandle schedule(Runnable action, Duration delay) {
        return delegate.schedule(action, delay);
    }

    @Override
    public EarlyFinishTrigger createEarlyFinishTrigger() {
        return delegate.createEarlyFinishTrigger();
    }
}
