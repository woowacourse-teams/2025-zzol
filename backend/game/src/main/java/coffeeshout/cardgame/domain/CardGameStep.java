package coffeeshout.cardgame.domain;

import coffeeshout.room.domain.Room;

public enum CardGameStep {

    START_ROUND {
        @Override
        public void execute(CardGame cardGame, Room room) {
            cardGame.startRound();
        }
    },
    PREPARE {
        @Override
        public void execute(CardGame cardGame, Room room) {
            cardGame.updateDescription();
        }
    },
    START_PLAY {
        @Override
        public void execute(CardGame cardGame, Room room) {
            cardGame.startPlay();
        }
    },
    FINISH_ROUND {
        @Override
        public void execute(CardGame cardGame, Room room) {
            cardGame.assignRandomCardsToUnselectedPlayers();
            cardGame.changeScoreBoardState();
        }
    },
    FINISH_GAME {
        @Override
        public void execute(CardGame cardGame, Room room) {
            cardGame.changeDoneState();
        }
    },
    ;

    public abstract void execute(CardGame cardGame, Room room);
}
