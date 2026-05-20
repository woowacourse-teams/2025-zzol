package coffeeshout.blockstacking.infra;

import coffeeshout.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BlockStackingStreamKey implements StreamKey {
    EVENTS("blockstacking");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
