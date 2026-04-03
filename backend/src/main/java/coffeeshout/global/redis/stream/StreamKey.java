package coffeeshout.global.redis.stream;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamKey {
    ROOM_BROADCAST("room"),
    ROOM_JOIN("room:join"),
    CARD_GAME_SELECT_BROADCAST("cardgame:select"),
    MINIGAME_EVENTS("minigame"),
    RACING_GAME_EVENTS("racinggame"),
    SPEED_TOUCH_EVENTS("speedtouch"),
    BLIND_TIMER_EVENTS("blindtimer"),
    BOMB_RELAY_EVENTS("bombrelay"),
    BLOCK_STACKING_EVENTS("blockstacking"),
    ;

    private final String redisKey;

    public static StreamKey fromRedisKey(String redisKey) {
        return Arrays.stream(values())
                .filter(key -> key.redisKey.equals(redisKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 Redis Stream 키: " + redisKey));
    }
}
