package coffeeshout.numberpoker.application.port;

import coffeeshout.global.flow.EarlyFinishTrigger;
import coffeeshout.global.flow.FlowHandle;
import java.time.Duration;

public interface NumberPokerFlowScheduler {

    FlowHandle schedule(Runnable action, Duration delay);

    EarlyFinishTrigger createEarlyFinishTrigger();
}
