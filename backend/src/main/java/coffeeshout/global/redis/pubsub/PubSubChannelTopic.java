package coffeeshout.global.redis.pubsub;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;

@RequiredArgsConstructor
@Getter
public enum PubSubChannelTopic {

    ROOM("room.events"),
    MINIGAME("minigame.events"),
    PLAYER("player.events"),
    SESSION("session.events"),
    RACING_GAME("racingGame.events"),
    ;

    private final String topicName;

    public ChannelTopic channelTopic() {
        return new ChannelTopic(topicName);
    }

}
