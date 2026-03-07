package coffeeshout.cardgame.application.port;

public interface EarlyFinishTrigger {

    void complete();

    boolean isCompleted();
}
