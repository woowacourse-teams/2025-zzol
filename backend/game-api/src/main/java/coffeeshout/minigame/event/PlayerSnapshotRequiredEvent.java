package coffeeshout.minigame.event;

/**
 * 첫 게임 시작 시 방 플레이어 스냅샷({@code PlayerEntity})을 영속화해야 함을 알리는 <b>in-process 동기</b> 이벤트.
 *
 * <p>{@code PlayerEntity} 생성은 {@code :room}의 {@code Player} 도메인(특히 {@code PlayerType})에 접근해야 하므로
 * 생성 책임을 {@code :room}이 소유한다(ADR-0025 — PlayerEntity 영속 책임 분리). {@code :game}은 게임 시작 영속 흐름
 * ({@code MiniGamePersistenceService.saveGameEntities})에서 이 이벤트를 발행만 하고, {@code :room}의
 * {@code PlayerSnapshotListener}가 동기 수신해 스냅샷을 저장한다.
 *
 * <p>동기 리스너이므로 발행자의 {@code @Transactional}+{@code @RedisLock} 안에서 실행된다 — 기존 직접 생성과
 * 동일한 순서·멱등·실패 전파를 보장하면서 {@code :game}의 {@code PlayerEntity} 의존만 제거한다.
 */
public record PlayerSnapshotRequiredEvent(String joinCode) {
}
