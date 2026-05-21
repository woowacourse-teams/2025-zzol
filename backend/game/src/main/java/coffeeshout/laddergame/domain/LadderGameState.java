package coffeeshout.laddergame.domain;

public enum LadderGameState {
    DESCRIPTION,
    PREPARE,
    DRAWING,
    RESULT,
    DONE;

    public boolean canTransitionTo(LadderGameState next) {
        return switch (this) {
            case DESCRIPTION -> next == PREPARE;
            case PREPARE -> next == DRAWING;
            case DRAWING -> next == RESULT;
            case RESULT -> next == DONE;
            case DONE -> false;
        };
    }
}
