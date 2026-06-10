package coffeeshout.minigame.event;

/**
 * GameSession이 {@code PLAYING}으로 전이됐음을 알리는 <b>in-process 동기</b> 이벤트(ADR-0023 결정 4).
 *
 * <p>게임 시작 처리({@code MiniGameEventService.onGameStartReady})에서 {@code startGame} 직후,
 * 실패 가능 I/O({@code miniGameService.start}·결과 영속)보다 <b>먼저</b> 발행한다. {@code :room}이 동기 수신해
 * 방을 {@code markPlaying} 하므로, GameSession과 Room의 PLAYING 전이가 한 묶음으로 끝난다. 이후 I/O가 실패해도
 * 두 상태가 모두 PLAYING으로 일관되게 남아(찢어진 상태 방지) 재시작 경로의 불변식이 깨지지 않는다.
 *
 * <p>사용자 대상 "게임 시작" 브로드캐스트는 별도의 {@link MiniGameStartedEvent}(I/O 성공 후 발행)가 담당한다.
 */
public record GameSessionStartedEvent(String joinCode) {
}
