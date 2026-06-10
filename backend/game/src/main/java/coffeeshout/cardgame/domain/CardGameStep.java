package coffeeshout.cardgame.domain;

public enum CardGameStep {

    START_ROUND {
        @Override
        public void execute(CardGame cardGame) {
            cardGame.startRound();
        }
    },
    PREPARE {
        @Override
        public void execute(CardGame cardGame) {
            cardGame.updateDescription();
        }
    },
    START_PLAY {
        @Override
        public void execute(CardGame cardGame) {
            cardGame.startPlay();
        }
    },
    FINISH_ROUND {
        @Override
        public void execute(CardGame cardGame) {
            cardGame.assignRandomCardsToUnselectedPlayers();
            cardGame.changeScoreBoardState();
        }
    },
    FINISH_GAME {
        @Override
        public void execute(CardGame cardGame) {
            cardGame.changeDoneState();
        }
    },
    ;

    public abstract void execute(CardGame cardGame);
}
