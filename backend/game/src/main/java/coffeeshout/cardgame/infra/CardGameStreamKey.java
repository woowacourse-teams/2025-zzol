package coffeeshout.cardgame.infra;

import coffeeshout.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CardGameStreamKey implements StreamKey {
    SELECT_BROADCAST("cardgame:select");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
