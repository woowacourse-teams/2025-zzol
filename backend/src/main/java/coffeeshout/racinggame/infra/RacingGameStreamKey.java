package coffeeshout.racinggame.infra;

import coffeeshout.global.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RacingGameStreamKey implements StreamKey {
    EVENTS("racinggame");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
