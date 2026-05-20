package coffeeshout.room.infra.messaging;

import coffeeshout.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RoomStreamKey implements StreamKey {
    BROADCAST("room"),
    JOIN("room:join");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
