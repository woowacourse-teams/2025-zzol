package coffeeshout.global.redis.config;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "redis.stream")
public record RedisStreamProperties(
        CommonSettings commonSettings,
        Map<String, ThreadPoolConfig> threadPools,
        Map<String, StreamConfig> keys
) {
    public record CommonSettings(
            @Positive int maxLength,
            @Positive int batchSize,
            Duration pollTimeout
    ) {
    }

    public record StreamConfig(
            String threadPoolName,
            ThreadPoolConfig threadPool,
            Integer maxLength,
            Integer batchSize,
            Duration pollTimeout
    ) {
        public StreamConfig {
            if (threadPoolName == null && threadPool == null) {
                throw new IllegalArgumentException(
                        "threadPoolName 또는 threadPool 중 하나는 반드시 지정해야 합니다."
                );
            }
            if (threadPoolName != null && threadPool != null) {
                throw new IllegalArgumentException(
                        "threadPoolName과 threadPool을 동시에 지정할 수 없습니다."
                );
            }
        }

        public boolean isUseSharedThreadPool() {
            return threadPoolName != null;
        }

        public int getMaxLength(CommonSettings common) {
            return maxLength != null ? maxLength : common.maxLength();
        }

        public int getBatchSize(CommonSettings common) {
            return batchSize != null ? batchSize : common.batchSize();
        }

        public Duration getPollTimeout(CommonSettings common) {
            return pollTimeout != null ? pollTimeout : common.pollTimeout();
        }
    }

    public record ThreadPoolConfig(
            @Positive int coreSize,
            @Positive int maxSize,
            @Positive int queueCapacity
    ) {
    }
}
