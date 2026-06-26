package coffeeshout.zzolbot.remediation.agent;

/**
 * 스택트레이스에서 결정적으로 특정한 결함 위치. LLM 없이 프레임 파싱으로 얻는다 — 토큰을 아끼고,
 * "어느 파일을 고칠지"라는 안전에 직결되는 판단을 LLM 환각에 맡기지 않기 위함이다.
 *
 * @param classFqn     결함 클래스 FQN(예: coffeeshout.room.application.RoomService)
 * @param methodName   결함 메서드명
 * @param filePath     repo 루트 기준 상대 경로(예: backend/room/src/main/java/coffeeshout/room/application/RoomService.java)
 * @param lineNumber   스택 프레임의 줄 번호
 * @param gradleModule 영향 모듈의 Gradle 경로(예: :room). 그 모듈 테스트만 돌려 검증 비용을 줄인다.
 */
public record DefectLocation(
        String classFqn,
        String methodName,
        String filePath,
        int lineNumber,
        String gradleModule) {
}
