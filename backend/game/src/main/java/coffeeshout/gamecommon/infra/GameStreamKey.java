package coffeeshout.gamecommon.infra;

import coffeeshout.redis.stream.StreamKey;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GameStreamKey implements StreamKey {
    BLINDTIMER_EVENTS("blindtimer"),
    BLOCKSTACKING_EVENTS("blockstacking"),
    CARDGAME_SELECT_BROADCAST("cardgame:select"),
    LADDERGAME_EVENTS("laddergame"),
    RACINGGAME_EVENTS("racinggame"),
    SPEEDTOUCH_EVENTS("speedtouch");

    private final String redisKey;

    @Override
    public String getRedisKey() {
        return redisKey;
    }
}
