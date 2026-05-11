package coffeeshout.minigame.infra;

import coffeeshout.global.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MinigameStreamKey implements StreamKey {
    EVENTS("minigame");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
