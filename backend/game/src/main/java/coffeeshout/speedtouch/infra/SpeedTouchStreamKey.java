package coffeeshout.speedtouch.infra;

import coffeeshout.global.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SpeedTouchStreamKey implements StreamKey {
    EVENTS("speedtouch");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
