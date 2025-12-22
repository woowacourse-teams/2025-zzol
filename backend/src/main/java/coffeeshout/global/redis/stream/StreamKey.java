package coffeeshout.global.redis.stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamKey {
    ROOM_BROADCAST("room"),
    ROOM_JOIN("room:join"),
    CARD_GAME_SELECT_BROADCAST("cardgame:select"),
    MINIGAME_EVENTS("minigame"),
    RACING_GAME_EVENTS("racinggame");

    private final String redisKey;
}
