package coffeeshout.laddergame.infra;

import coffeeshout.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LadderGameStreamKey implements StreamKey {
    EVENTS("laddergame");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
