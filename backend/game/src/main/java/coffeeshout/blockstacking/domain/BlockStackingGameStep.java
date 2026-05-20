package coffeeshout.blockstacking.domain;

import coffeeshout.room.domain.Room;

public enum BlockStackingGameStep {

    PREPARE {
        @Override
        public void execute(BlockStackingGame game, Room room) {
            game.prepare();
        }
    },
    START_PLAY {
        @Override
        public void execute(BlockStackingGame game, Room room) {
            game.startPlay();
        }
    },
    FINISH_GAME {
        @Override
        public void execute(BlockStackingGame game, Room room) {
            game.finish();
            room.applyMiniGameResult(game.getResult());
        }
    },
    ;

    public abstract void execute(BlockStackingGame game, Room room);
}
