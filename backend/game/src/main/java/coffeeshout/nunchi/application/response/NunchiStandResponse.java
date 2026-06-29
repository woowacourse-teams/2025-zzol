package coffeeshout.nunchi.application.response;

/**
 * 한 명이 번호를 차지(첫 press 즉시·낙관적, ADR-0031 N2/Q3-b). {@code number}는 카운터 값이지 등수가
 * 아니며 rank는 싣지 않는다(결정 8). {@code idleDeadlineEpochMs}는 이 유효 입력으로 리셋된 무입력 종료
 * 예정 시각, {@code serverNowEpochMs}는 클라 시계 스큐 보정용 서버 현재 시각이다.
 */
public record NunchiStandResponse(
        String name,
        int number,
        long serverNowEpochMs,
        long idleDeadlineEpochMs
) {
}
