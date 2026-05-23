package coffeeshout.profanity.infra.redis;

import coffeeshout.profanity.domain.TrieRefreshPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfanityTrieRefreshPublisher implements TrieRefreshPort {

    static final String CHANNEL = "profanity:trie:refresh";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void publish() {
        redisTemplate.convertAndSend(CHANNEL, "refresh");
    }
}
