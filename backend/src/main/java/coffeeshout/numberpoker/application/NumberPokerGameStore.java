package coffeeshout.numberpoker.application;

import coffeeshout.global.exception.custom.BusinessException;
import coffeeshout.numberpoker.domain.NumberPokerErrorCode;
import coffeeshout.numberpoker.domain.NumberPokerGame;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class NumberPokerGameStore {

    private final ConcurrentHashMap<String, NumberPokerGame> games = new ConcurrentHashMap<>();

    public void save(String joinCode, NumberPokerGame game) {
        games.put(joinCode, game);
    }

    public NumberPokerGame get(String joinCode) {
        final NumberPokerGame game = games.get(joinCode);
        if (game == null) {
            throw new BusinessException(
                    NumberPokerErrorCode.GAME_NOT_FOUND,
                    "진행 중인 넘버포커 게임을 찾을 수 없습니다. joinCode=" + joinCode
            );
        }
        return game;
    }

    public void remove(String joinCode) {
        games.remove(joinCode);
    }
}
