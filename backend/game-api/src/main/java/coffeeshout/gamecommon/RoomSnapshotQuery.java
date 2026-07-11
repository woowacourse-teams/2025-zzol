package coffeeshout.gamecommon;

import java.util.List;

/**
 * 게임 결과 영속에 필요한 방·플레이어 식별자를 {@code joinCode}·이름으로 조회하는 SPI.
 *
 * <p>{@code :game}의 미니게임 영속 엔티티({@code mini_game_play}·{@code mini_game_result})는 방·플레이어를
 * JPA 연관이 아니라 {@code Long} FK 컬럼으로 참조한다(ADR-0034). 그 id를 {@code :room}이 이 포트로 공급하고
 * {@code :room}이 구현체를 제공한다 — {@code :game}은 {@code :room}을 직접 참조하지 않는다(의존 역전).
 */
public interface RoomSnapshotQuery {

    /**
     * joinCode에 해당하는 최신 방 세션(room_session)의 id를 반환한다. 없으면 예외를 던진다.
     */
    long resolveRoomSessionId(String joinCode);

    /**
     * 주어진 방 세션의 플레이어 중 이름이 일치하는 것들의 식별 정보를 반환한다.
     */
    List<PlayerSnapshot> resolvePlayers(long roomSessionId, List<String> playerNames);

    record PlayerSnapshot(String playerName, long playerId) {
    }
}
