package coffeeshout.blockstacking.application;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameType;
import org.springframework.stereotype.Component;

@Component
public class BlockStackingGameFactory implements MiniGameFactory {

    @Override
    public MiniGameType type() {
        return MiniGameType.BLOCK_STACKING;
    }

    @Override
    public Playable create(String joinCode) {
        return new BlockStackingGame();
    }
}
