package coffeeshout.global.redis.config;

import coffeeshout.fixture.BaseEventDummy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class RedisStreamResilienceTestConfig {

    public static final String THROW_PREFIX = "THROW:";

    @Bean
    public RecordingDummyEventConsumer recordingDummyEventConsumer() {
        return new RecordingDummyEventConsumer();
    }

    public static class RecordingDummyEventConsumer implements Consumer<BaseEventDummy> {

        private final List<String> payloads = new CopyOnWriteArrayList<>();

        @Override
        public void accept(BaseEventDummy event) {
            if (event.payload().startsWith(THROW_PREFIX)) {
                throw new RuntimeException("의도된 Consumer 실패: " + event.payload());
            }
            payloads.add(event.payload());
        }

        public List<String> payloads() {
            return List.copyOf(payloads);
        }
    }
}
