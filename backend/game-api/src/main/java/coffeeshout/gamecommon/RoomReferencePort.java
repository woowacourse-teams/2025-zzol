package coffeeshout.gamecommon;

import java.util.List;
import java.util.Optional;

/**
 * {@code :game} 영속 계층이 {@code :room}의 식별·상태를 참조하기 위한 포트. {@code :room}이 구현하고
 * {@code :game}이 소비한다(room → game-api 의존만으로 성립).
 *
 * <p>MiniGameEntity의 RoomEntity FK·MiniGameResultEntity의 PlayerEntity FK를 ID 참조로 분리하면서,
 * 게임 측이 {@code RoomEntity}/{@code PlayerEntity} 구체 타입을 모른 채 필요한 식별자
 * (roomSessionId·playerId·userId)만 받도록 한다(ADR-0025 FK 영속 책임 분리 후속).
 *
 * <p>{@code GameRoomCreatedEvent} 등과 같이 {@code :room}이 생산하고 {@code :game}이 소비하는
 * {@code gamecommon} 계약 계열이다.
 */
public interface RoomReferencePort {

    /**
     * joinCode에 해당하는 현재(가장 최근 생성) RoomSession의 ID를 조회한다. "어느 세션이 현재인가"의
     * 판정은 {@code :room}이 소유한다 — joinCode 하나에 세션이 시간순으로 여러 개 존재할 수 있으므로
     * joinCode를 그대로 FK로 들 수 없고 현재 세션 ID로 환원해야 한다.
     */
    Optional<Long> findCurrentRoomSessionId(String joinCode);

    /**
     * 현재 RoomSession의 영속 상태를 PLAYING으로 전이한다. {@code RoomState} 열거는 {@code :room}
     * 내부에 가둔다.
     */
    void markRoomPlaying(String joinCode);

    /**
     * roomSessionId에 속한 플레이어 중 이름이 일치하는 참조(id·name·userId)를 조회한다.
     */
    List<PlayerRef> findPlayerRefs(Long roomSessionId, List<String> playerNames);
}
