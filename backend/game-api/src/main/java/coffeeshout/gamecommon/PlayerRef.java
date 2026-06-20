package coffeeshout.gamecommon;

/**
 * {@code :game}이 결과 영속에 필요로 하는 플레이어 참조의 최소 표현. {@code PlayerEntity} 구체 타입을
 * {@code :game}에 노출하지 않기 위한 DTO다.
 *
 * <ul>
 *   <li>{@code id} — {@code mini_game_result.player_id} FK 값</li>
 *   <li>{@code name} — score 맵 키(Gamer 이름)와 매칭하는 플레이어 이름</li>
 *   <li>{@code userId} — 회원이면 UserStats 갱신에 사용, 게스트면 {@code null}</li>
 * </ul>
 */
public record PlayerRef(Long id, String name, Long userId) {
}
