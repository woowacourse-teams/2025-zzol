package coffeeshout.global.redis.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Redis Stream 설정. 공통 설정과 스트림 키가 없으면 발행할 수 없으므로, 요청이 들어온 뒤가 아니라 기동할 때 실패한다.
 */
@Validated
@ConfigurationProperties(prefix = "redis.stream")
public record RedisStreamProperties(
        @NotNull CommonSettings commonSettings,
        Map<String, ThreadPoolConfig> threadPools,
        @NotEmpty Map<String, StreamConfig> keys) {

    /**
     * 스트림에 남길 최대 길이. 스트림에 따로 정하지 않았으면 공통 설정을 따른다.
     *
     * @throws IllegalArgumentException 설정에 없는 스트림 키일 때
     */
    public int maxLength(String redisKey) {
        final StreamConfig streamConfig = keys.get(redisKey);
        if (streamConfig == null) {
            throw new IllegalArgumentException("존재하지 않는 키입니다: " + redisKey + ". 사용 가능한 키: " + keys.keySet());
        }
        return streamConfig.getMaxLength(commonSettings);
    }

    public record CommonSettings(
            @Positive int maxLength,
            @Positive int batchSize,
            @NotNull Duration pollTimeout,
            @DefaultValue("5s") @NotNull Duration subscriptionStartTimeout) {}

    public record StreamConfig(
            String threadPoolName,
            ThreadPoolConfig threadPool,
            Integer maxLength,
            Integer batchSize,
            Duration pollTimeout,
            Boolean listenerEnabled) {
        public StreamConfig {
            final boolean enabled = listenerEnabled == null || listenerEnabled;
            if (enabled) {
                if (threadPoolName == null && threadPool == null) {
                    throw new IllegalArgumentException("threadPoolName 또는 threadPool 중 하나는 반드시 지정해야 합니다.");
                }
                if (threadPoolName != null && threadPool != null) {
                    throw new IllegalArgumentException("threadPoolName과 threadPool을 동시에 지정할 수 없습니다.");
                }
            } else if (threadPoolName != null || threadPool != null) {
                // 리스너를 만들지 않는 스트림은 소비 스레드가 없다 — 풀 지정은 설정 오해의 신호다.
                throw new IllegalArgumentException("listener-enabled: false인 스트림은 스레드풀을 지정할 수 없습니다.");
            }
        }

        /**
         * 브로드캐스트 리스너({@code RedisStreamListenerStarter})를 생성할지 여부. 기본 true.
         * <p>
         * false인 스트림은 발행({@code StreamPublisher})만 이 설정을 통해 허용되고, 소비는 별도
         * 컨슈머 그룹 소유 컴포넌트가 담당한다. 전 인스턴스 브로드캐스트가 아니라 단일 처리
         * 경로가 필요한 작업 큐 스트림(예: 시즌 정산)이 여기에 해당한다(#1610).
         */
        public boolean isListenerEnabled() {
            return listenerEnabled == null || listenerEnabled;
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
            @Positive int queueCapacity) {}
}
