package coffeeshout.report.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ReportCategory {
    BUG("버그"),
    SUGGESTION("건의사항"),
    GAME_REQUEST("게임 요청"),
    OTHER("기타"),
    ;

    public final String label;
}
