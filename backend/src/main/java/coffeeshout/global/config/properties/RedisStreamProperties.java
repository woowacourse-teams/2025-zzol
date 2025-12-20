package coffeeshout.global.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.data.redis.stream")
public record RedisStreamProperties(
        Map<String, ThreadPoolConfig> threadPools,
        List<StreamConfig> streams,
        @Positive int maxLength,
        @Positive int batchSize,
        Duration pollTimeout
) {
    public record StreamConfig(
            @NotBlank String name,
            @NotBlank String key,
            String threadPoolName,
            ThreadPoolConfig threadPool
    ) {
        public StreamConfig {
            if (threadPoolName == null && threadPool == null) {
                throw new IllegalArgumentException(
                        "스트림 '" + name + "': threadPoolName 또는 threadPool 중 하나는 반드시 지정해야 합니다."
                );
            }
            if (threadPoolName != null && threadPool != null) {
                throw new IllegalArgumentException(
                        "스트림 '" + name + "': threadPoolName과 threadPool을 동시에 지정할 수 없습니다."
                );
            }
        }
    }

    public record ThreadPoolConfig(
            @Positive int coreSize,
            @Positive int maxSize,
            @Positive int queueCapacity
    ) {
    }

    public ThreadPoolConfig getThreadPoolConfig(String streamName) {
        StreamConfig stream = streams.stream()
                .filter(s -> s.name().equals(streamName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Stream not found: " + streamName));

        if (stream.threadPoolName() != null) {
            ThreadPoolConfig config = threadPools.get(stream.threadPoolName());
            if (config == null) {
                throw new IllegalArgumentException(
                        "ThreadPool not found: " + stream.threadPoolName()
                );
            }
            return config;
        }

        return stream.threadPool();
    }

    public boolean isUsingSharedThreadPool(String streamName) {
        StreamConfig stream = streams.stream()
                .filter(s -> s.name().equals(streamName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Stream not found: " + streamName));

        return stream.threadPoolName() != null;
    }
}
