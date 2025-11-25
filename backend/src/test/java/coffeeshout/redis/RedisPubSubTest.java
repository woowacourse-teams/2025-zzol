package coffeeshout.redis;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.ServiceTest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RedisPubSubTest extends ServiceTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Test
    void Redis_PubSub이_정상_동작한다() throws InterruptedException {
        // given
        final String testTopic = "test.topic";
        final String testMessage = "Hello Redis";
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> receivedMessage = new AtomicReference<>();

        final MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                receivedMessage.set(new String(message.getBody()));
                latch.countDown();
            }
        };

        // when
        redisMessageListenerContainer.addMessageListener(listener, new ChannelTopic(testTopic));

        // 리스너 등록을 위해 잠시 대기
        Thread.sleep(100);

        redisTemplate.convertAndSend(testTopic, testMessage);

        // then
        final boolean messageReceived = latch.await(5, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(receivedMessage.get()).isEqualTo("\"" + testMessage + "\"");
    }
}
