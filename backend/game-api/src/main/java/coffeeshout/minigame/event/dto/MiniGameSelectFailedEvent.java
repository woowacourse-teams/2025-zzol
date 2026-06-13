package coffeeshout.minigame.event.dto;

/**
 * 미니게임 선택 반영이 비동기 Consumer에서 검증 실패했을 때 발행하는 in-process 이벤트.
 * {@code :room}의 리스너가 수신해 {@code principalName} 클라이언트에게만 에러를 전송한다
 * (Stream을 경유하는 명령 처리 흐름에서 동기 응답이 불가능하므로 실패도 이벤트로 되돌린다 — ADR-0025).
 */
public record MiniGameSelectFailedEvent(
        String joinCode,
        String principalName,
        String errorMessage
) {
}
