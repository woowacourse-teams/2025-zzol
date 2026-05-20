package coffeeshout.blindtimer.infra;

import coffeeshout.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BlindTimerStreamKey implements StreamKey {
    EVENTS("blindtimer");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
