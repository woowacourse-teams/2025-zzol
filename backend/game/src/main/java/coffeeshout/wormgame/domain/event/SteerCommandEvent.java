package coffeeshout.wormgame.domain.event;

import coffeeshout.global.redis.BaseEvent;
import java.time.Instant;
import java.util.UUID;

/** 조향 커맨드 — 목표각 하나와 클라 예측 보정용 일련번호. 마지막 값만 유효하므로 유실·역전에 안전하다. */
public record SteerCommandEvent(
        String eventId, String joinCode, String playerName, double angle, long seq, Instant timestamp)
        implements BaseEvent {

    public static SteerCommandEvent create(String joinCode, String playerName, double angle, long seq) {
        return new SteerCommandEvent(UUID.randomUUID().toString(), joinCode, playerName, angle, seq, Instant.now());
    }
}
