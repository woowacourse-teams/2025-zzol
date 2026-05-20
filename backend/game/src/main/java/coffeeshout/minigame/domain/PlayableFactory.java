package coffeeshout.minigame.domain;

public interface PlayableFactory {

    Playable create(MiniGameType type, String joinCode);
}
