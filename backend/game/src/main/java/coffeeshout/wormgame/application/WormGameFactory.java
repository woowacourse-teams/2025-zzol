package coffeeshout.wormgame.application;

import coffeeshout.gamecommon.MiniGameFactory;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.wormgame.config.WormGameRulesProperties;
import coffeeshout.wormgame.domain.WormGame;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WormGameFactory implements MiniGameFactory {

    private final WormGameRulesProperties rulesProperties;

    @Override
    public MiniGameType type() {
        return MiniGameType.WORM_GAME;
    }

    @Override
    public Playable create(String joinCode) {
        return new WormGame(rulesProperties.toRules());
    }
}
