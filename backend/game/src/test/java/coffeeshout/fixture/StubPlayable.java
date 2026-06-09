package coffeeshout.fixture;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import java.util.List;
import java.util.Map;

/**
 * 게임 타입을 파라미터로 받고 {@link #setUp} 호출을 기록하는 {@link Playable} 테스트 더블.
 * 기존 {@link MiniGameDummy}는 CARD_GAME 고정이라 여러 타입·setUp 추적이 필요한 GameSession 테스트에 부적합하다.
 */
public class StubPlayable implements Playable {

    private final MiniGameType miniGameType;
    private boolean setUpCalled = false;
    private List<Gamer> setUpGamers = null;

    public StubPlayable(MiniGameType miniGameType) {
        this.miniGameType = miniGameType;
    }

    @Override
    public MiniGameResult getResult() {
        return MiniGameResult.fromDescending(getScores());
    }

    @Override
    public Map<Gamer, MiniGameScore> getScores() {
        return Map.of();
    }

    @Override
    public MiniGameType getMiniGameType() {
        return miniGameType;
    }

    @Override
    public void setUp(List<Gamer> gamers) {
        this.setUpCalled = true;
        this.setUpGamers = gamers;
    }

    public boolean isSetUpCalled() {
        return setUpCalled;
    }

    public List<Gamer> getSetUpGamers() {
        return setUpGamers;
    }
}
