package coffeeshout.blockstacking.domain;

public enum BlockStackingGameStep {

    PREPARE {
        @Override
        public void execute(BlockStackingGame game) {
            game.prepare();
        }
    },
    START_PLAY {
        @Override
        public void execute(BlockStackingGame game) {
            game.startPlay();
        }
    },
    FINISH_GAME {
        @Override
        public void execute(BlockStackingGame game) {
            game.finish();
        }
    },
    ;

    public abstract void execute(BlockStackingGame game);
}
