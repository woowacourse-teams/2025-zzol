package coffeeshout.profanity.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfanityTrieRefreshPublisher {

    static final String CHANNEL = "profanity:trie:refresh";

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish() {
        redisTemplate.convertAndSend(CHANNEL, "refresh");
    }
}
