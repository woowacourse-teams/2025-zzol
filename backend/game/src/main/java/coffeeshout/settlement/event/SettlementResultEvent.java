package coffeeshout.settlement.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.List;

/**
 * 미니게임 종료 시 시즌 정산을 위해 발행하는 이벤트. 회원 플레이어의 결과만 담는다
 * (게스트는 영속 식별자가 없어 시즌 포인트를 누적할 수 없다).
 * <p>
 * {@code eventId}는 랜덤 UUID가 아니라 {@code roomSessionId + miniGameType}에서 안정적으로
 * 파생한다. 같은 게임 종료가 blue/green 양쪽에서 발행되거나 Outbox가 재발행해도 항상 같은
 * ID가 되어, 컨슈머의 멱등 처리 기준으로 쓸 수 있다(#1610 — {@code MiniGameFinishedEvent}의
 * 인스턴스별 랜덤 eventId를 정산에 쓰지 않는 이유).
 */
public record SettlementResultEvent(
        String eventId,
        Instant timestamp,
        String joinCode,
        long roomSessionId,
        String miniGameType,
        List<PlayerResult> results
) implements BaseEvent {

    public SettlementResultEvent {
        results = List.copyOf(results);
    }

    public static SettlementResultEvent of(
            String joinCode,
            long roomSessionId,
            String miniGameType,
            List<PlayerResult> results
    ) {
        return new SettlementResultEvent(
                deriveEventId(roomSessionId, miniGameType),
                Instant.now(),
                joinCode,
                roomSessionId,
                miniGameType,
                results
        );
    }

    private static String deriveEventId(long roomSessionId, String miniGameType) {
        return "settlement:" + roomSessionId + ":" + miniGameType;
    }

    public record PlayerResult(long userId, String playerName, int rank, long score) {
    }
}
