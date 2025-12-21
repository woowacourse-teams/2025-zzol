package coffeeshout.global.redis.stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamKey {
    ROOM_BROADCAST("room:broadcast"),
    CARD_GAME_SELECT_BROADCAST("cardGame:select:broadcast");

    private final String redisKey;
}
