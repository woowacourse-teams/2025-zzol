package coffeeshout.cardgame.application.port;

import java.time.Duration;

public interface CardGameFlowScheduler {

    FlowHandle schedule(Runnable action, Duration delay);

    EarlyFinishTrigger createEarlyFinishTrigger();
}
