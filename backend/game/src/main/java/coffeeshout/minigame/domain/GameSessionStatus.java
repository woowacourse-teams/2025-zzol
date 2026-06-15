package coffeeshout.minigame.domain;

public enum GameSessionStatus {

    READY,      // 게임 선택·변경 가능
    PLAYING,    // 게임 진행 중 — 대기열 변경 불가
    DONE,       // 모든 게임 완료
}
