package coffeeshout.minigame.event;

import coffeeshout.gamecommon.Gamer;
import java.util.List;

/**
 * 방 검증을 통과해 게임을 시작할 준비가 됐음을 알리는 <b>in-process 동기</b> 이벤트(ADR-0025 결정 4).
 *
 * <p>{@code :room}이 {@code StartMiniGameCommandEvent}를 검증한 뒤 발행하고, {@code :game}이 동기 수신해
 * GameSession을 시작한다. Redis Stream이 아닌 in-process로 두는 이유:
 * <ul>
 *   <li>스트림 리스너가 컨슈머 그룹을 쓰지 않아(각 인스턴스가 모든 메시지를 독립 소비) 중간 단계를 스트림으로
 *       쪼개면 인스턴스 수만큼 중복 발행돼 대기열이 깨진다. in-process는 발행 인스턴스에서 한 번만 동기 실행된다.</li>
 *   <li>동기 실행이라 {@code startGame} 실패가 발행 측으로 전파돼, 방 {@code markPlaying} 전이를 건너뛸 수 있다
 *       (검증 → 시작 → PLAYING 전이의 원자성·순서 보존).</li>
 * </ul>
 * 발행 인스턴스와 동일 인스턴스에서 처리되므로 플레이어 명단({@link Gamer})을 원시 직렬화 없이 그대로 싣는다.
 * {@code eventId}는 원본 커맨드의 식별자로, 게임 결과 영속의 멱등 키({@code @RedisLock})로 재사용된다.
 */
public record GameStartReadyEvent(
        String eventId,
        String joinCode,
        String hostName,
        List<Gamer> gamers
) {
}
