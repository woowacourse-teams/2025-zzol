package coffeeshout.profanity.infra.redis;

import coffeeshout.profanity.application.ProfanityFilterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfanityTrieRefreshSubscriber implements MessageListener {

    private final RedisMessageListenerContainer container;
    private final ProfanityFilterService filterService;

    @PostConstruct
    public void register() {
        container.addMessageListener(this, new PatternTopic(ProfanityRedisChannel.TRIE_REFRESH));
        log.info("비속어 트라이 갱신 구독 등록 완료 — channel: {}", ProfanityRedisChannel.TRIE_REFRESH);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.debug("비속어 트라이 갱신 신호 수신");
        filterService.rebuildTrie();
    }
}
