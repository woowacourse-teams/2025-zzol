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
        List<ChannelConfig> channels,
        @Positive int maxLength,
        @Positive int batchSize,
        Duration pollTimeout
) {
    public record ChannelConfig(
            @NotBlank String name,
            @NotBlank String key,
            String threadPoolName,
            ThreadPoolConfig threadPool
    ) {
        public ChannelConfig {
            if (threadPoolName == null && threadPool == null) {
                throw new IllegalArgumentException(
                        "채널 '" + name + "': threadPoolName 또는 threadPool 중 하나는 반드시 지정해야 합니다."
                );
            }
            if (threadPoolName != null && threadPool != null) {
                throw new IllegalArgumentException(
                        "채널 '" + name + "': threadPoolName과 threadPool을 동시에 지정할 수 없습니다."
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

    public ThreadPoolConfig getThreadPoolConfig(String channelName) {
        ChannelConfig channel = channels.stream()
                .filter(c -> c.name().equals(channelName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelName));

        if (channel.threadPoolName() != null) {
            ThreadPoolConfig config = threadPools.get(channel.threadPoolName());
            if (config == null) {
                throw new IllegalArgumentException(
                        "ThreadPool not found: " + channel.threadPoolName()
                );
            }
            return config;
        }

        return channel.threadPool();
    }

    public boolean isUsingSharedThreadPool(String channelName) {
        ChannelConfig channel = channels.stream()
                .filter(c -> c.name().equals(channelName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelName));

        return channel.threadPoolName() != null;
    }
}
