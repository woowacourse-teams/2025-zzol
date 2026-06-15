package coffeeshout.profanity.infra.redis;

import coffeeshout.profanity.domain.TrieRefreshNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfanityTrieRefreshPublisher implements TrieRefreshNotifier {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void publish() {
        try {
            redisTemplate.convertAndSend(ProfanityRedisChannel.TRIE_REFRESH, "refresh");
        } catch (Exception e) {
            log.error("Failed to publish trie refresh to channel '{}': {}", ProfanityRedisChannel.TRIE_REFRESH, e.getMessage(), e);
        }
    }
}
