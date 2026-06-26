package coffeeshout.zzolbot.remediation.domain;

/**
 * 수정 시도(dispatch된 건)의 생애주기. 앱이 GitHub Actions 워커로 작업을 넘긴 순간 {@link #DISPATCHED}로
 * 기록되고, 워커가 내부 콜백으로 결과를 보고하면 나머지 상태로 전이한다.
 *
 * <p>게이트에서 막힌 경우(코드 결함 아님·예산 소진·쿨다운)는 시도 자체를 만들지 않고 트리거 응답으로만
 * 사유를 돌려준다. 즉 이 테이블에는 "실제로 워커에 넘긴 시도"만 쌓인다.
 */
public enum RemediationStatus {

    /**
     * 앱이 결함으로 분류해 GitHub Actions 워커로 작업을 넘긴 상태(검증·PR 결과 대기).
     */
    DISPATCHED,

    /**
     * 워커가 재현 테스트 RED→GREEN + 모듈 테스트 통과를 만족해 수정 PR을 열었다.
     */
    PR_OPENED,

    /**
     * 워커가 검증을 통과하는 수정을 끝내 만들지 못했다(PR 없음). 정상적 실패 — 봇은 틀린 PR을 열지 않는다.
     */
    NO_FIX,

    /**
     * 워커 실행 중 오류로 중단(빌드 환경·예외 등).
     */
    FAILED
}
