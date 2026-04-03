package coffeeshout.global.flow;

import java.time.Duration;

public interface FlowScheduler {

    FlowHandle schedule(Runnable action, Duration delay);

    EarlyFinishTrigger createEarlyFinishTrigger();
}
