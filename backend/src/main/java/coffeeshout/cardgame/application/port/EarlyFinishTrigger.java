package coffeeshout.cardgame.application.port;

import java.util.concurrent.CompletionStage;

public interface EarlyFinishTrigger {

    void complete();

    boolean isCompleted();

    CompletionStage<Void> asCompletionStage();
}
