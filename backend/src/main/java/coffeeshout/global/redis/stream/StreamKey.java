package coffeeshout.global.redis.stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamKey {
    ROOM_BROADCAST("room:broadcast"),
    ROOM_JOIN("room:join"),
    CARD_GAME_SELECT_BROADCAST("cardgame:select:broadcast"),
    MINIGAME_EVENTS("minigame:events"),
    RACING_GAME_EVENTS("racinggame:events");

    private final String redisKey;
}
