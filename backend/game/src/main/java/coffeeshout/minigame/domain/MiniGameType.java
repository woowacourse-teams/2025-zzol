package coffeeshout.minigame.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MiniGameType {

    CARD_GAME("카드게임"),
    RACING_GAME("레이싱"),
    SPEED_TOUCH("스피드터치"),
    BLIND_TIMER("블라인드타이머"),
    BLOCK_STACKING("블록쌓기"),
    LADDER_GAME("사다리타기"),
    ;

    public final String label;
}
