package coffeeshout.minigame.domain;

public interface MiniGameService {

    void start(String joinCode, String hostName);

    MiniGameType getMiniGameType();
}
