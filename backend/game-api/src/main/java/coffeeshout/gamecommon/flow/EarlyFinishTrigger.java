package coffeeshout.gamecommon.flow;

import java.util.concurrent.CompletionStage;

public interface EarlyFinishTrigger {

    void complete();

    boolean isCompleted();

    CompletionStage<Void> asCompletionStage();
}
