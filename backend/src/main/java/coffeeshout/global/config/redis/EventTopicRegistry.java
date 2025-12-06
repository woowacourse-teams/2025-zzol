package coffeeshout.global.config.redis;

import lombok.Getter;
import org.springframework.data.redis.listener.ChannelTopic;

@Getter
public enum EventTopicRegistry {
    ROOM("room.events"),
    MINI_GAME("minigame.events"),
    PLAYER("player.events"),
    SESSION("session.events"),
    RACING_GAME("racinggame.events");

    private final ChannelTopic channelTopic;

    EventTopicRegistry(String topicName) {
        this.channelTopic = new ChannelTopic(topicName);
    }

    public ChannelTopic toChannelTopic() {
        return channelTopic;
    }

    public String getTopic() {
        return channelTopic.getTopic();
    }
}
