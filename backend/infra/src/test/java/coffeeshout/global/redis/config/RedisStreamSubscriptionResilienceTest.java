package coffeeshout.global.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.InfraModuleIntegrationTest;
import coffeeshout.fixture.BaseEventDummy;
import coffeeshout.global.redis.config.RedisStreamResilienceTestConfig.RecordingDummyEventConsumer;
import coffeeshout.global.redis.stream.StreamPublisher;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 처리에 실패하는 메시지가 끼어도 Redis Stream 구독이 유지되어
 * 후속 메시지를 계속 소비하는지 검증한다.
 * RedisStreamResilienceTestConfig는 베이스 클래스(InfraModuleIntegrationTest)가 임포트한다.
 */
class RedisStreamSubscriptionResilienceTest extends InfraModuleIntegrationTest {

    private static final String STREAM_KEY = "minigame";

    @Autowired
    private StreamPublisher streamPublisher;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RecordingDummyEventConsumer consumer;

    @Test
    void 역직렬화_불가능한_메시지가_들어와도_구독이_유지되어_후속_메시지를_소비한다() {
        // given — 역직렬화에 실패하는 poison 메시지를 스트림에 직접 삽입
        stringRedisTemplate.opsForStream()
                .add(StreamRecords.newRecord().in(STREAM_KEY).ofObject("json이 아닌 poison 메시지"));

        // when — poison 이후 정상 이벤트 발행
        final BaseEventDummy event = BaseEventDummy.페이로드("poison-이후-정상-" + UUID.randomUUID());
        streamPublisher.publish(STREAM_KEY, event);

        // then — 구독이 살아있어 정상 이벤트가 소비된다
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(consumer.payloads()).contains(event.payload()));
    }

    @Test
    void Consumer가_예외를_던져도_구독이_유지되어_후속_메시지를_소비한다() {
        // given — Consumer가 예외를 던지는 이벤트 발행
        final BaseEventDummy failingEvent = BaseEventDummy.페이로드(
                RedisStreamResilienceTestConfig.THROW_PREFIX + UUID.randomUUID());
        streamPublisher.publish(STREAM_KEY, failingEvent);

        // when — 실패 이벤트 이후 정상 이벤트 발행
        final BaseEventDummy event = BaseEventDummy.페이로드("실패-이후-정상-" + UUID.randomUUID());
        streamPublisher.publish(STREAM_KEY, event);

        // then — 구독이 살아있어 정상 이벤트가 소비되고, 실패 이벤트는 기록되지 않는다
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(consumer.payloads()).contains(event.payload()));
        assertThat(consumer.payloads()).doesNotContain(failingEvent.payload());
    }
}
