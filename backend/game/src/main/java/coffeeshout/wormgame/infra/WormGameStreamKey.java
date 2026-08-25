package coffeeshout.wormgame.infra;

import coffeeshout.global.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum WormGameStreamKey implements StreamKey {
    EVENTS("wormgame");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
