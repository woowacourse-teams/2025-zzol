package coffeeshout.admin.quality.domain;

/**
 * 신고가 지금 얼마나 밀렸는가.
 *
 * <p>한때 처리 시간의 중앙값과 p95 도 함께 냈는데 걷어냈다. 백분위는 표본이 쌓여야 뜻이
 * 생기는 수인데 신고는 하루에 몇 건 수준이라, 신고 하나가 들어오고 나갈 때마다 두 수가
 * 크게 흔들렸다. 흔들리는 수를 화면에 두면 나머지 수까지 못 믿게 된다.
 *
 * @param pendingCount         아직 처리하지 않은 신고. 지금 손대야 할 건수다
 * @param oldestPendingMinutes 가장 오래 기다린 미처리 신고의 나이. 없으면 0.
 *                             적체 추이는 스냅샷이 없어 못 그리지만 이 값 하나로 "지금 밀렸나"는 답한다
 */
public record ReportBacklog(long pendingCount, long oldestPendingMinutes) {}
