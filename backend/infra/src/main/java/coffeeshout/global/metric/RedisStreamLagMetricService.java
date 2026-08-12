package coffeeshout.global.metric;

import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import coffeeshout.global.redis.config.RedisStreamThreadPoolConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Redis Stream의 공간적 백로그(lag)를 주기적으로 수집한다.
 *
 * <p>현재 ZZOL은 Consumer Group 없이 기동 시점 마지막 ID부터 소비하므로(ADR-0022),
 * XINFO GROUPS 대신 XLEN(스트림 길이)과 스레드풀 큐 깊이를 조합하여 측정한다.</p>
 *
 * <p>Redis 부하 최소화:
 * <ul>
 *   <li>XLEN은 O(1) 연산이므로 Redis에 거의 부하를 주지 않음</li>
 *   <li>스레드풀 큐 깊이는 JVM 내부 조회이므로 Redis 호출 없음</li>
 *   <li>Gauge는 Prometheus scrape 시점에만 평가되므로 불필요한 polling 없음</li>
 * </ul>
 * </p>
 *
 * <p>Prometheus 메트릭명:
 * <ul>
 *   <li>redis_stream_length (tag: stream)</li>
 *   <li>redis_stream_threadpool_queue_size (tag: stream)</li>
 *   <li>redis_stream_threadpool_active_count (tag: stream)</li>
 * </ul>
 * </p>
 *
 * <p>종료 시 Redis를 조회하지 않는다({@link SmartLifecycle}). {@code AbstractApplicationContext.doClose()}는
 * ① {@code ContextClosedEvent} 발행 → ② Lifecycle 빈 stop → ③ 빈 파괴 순으로 진행하고,
 * ①의 이벤트를 받은 {@code MeterRegistryCloser}가 마지막 publish를 수행하며 <b>모든 Gauge를 평가</b>한다.
 * 이벤트가 stop보다 <b>먼저</b>이므로, 평범한 close에서는 게이지 평가 시점에 커넥션 팩토리가 아직 살아 있다.</p>
 *
 * <p>문제가 되는 건 <b>close 이전에 이미 lifecycle이 멈춘 컨텍스트</b>다 — Spring Framework 7의 테스트
 * 컨텍스트 pause가 그렇다. pause가 lifecycle을 통째로 멈춘 뒤 JVM 종료 시점에 close가 오면, ①의 게이지
 * 평가가 이미 STOPPED인 {@code LettuceConnectionFactory}를 호출해
 * {@code IllegalStateException: ... has been STOPPED}가 스트림 수만큼 터진다(#1642).
 * {@code stop()}에서 내린 플래그가 그 호출 자체를 막아 NaN을 낸다.</p>
 */
@Slf4j
@Component
public class RedisStreamLagMetricService implements SmartLifecycle {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStreamProperties redisStreamProperties;
    private final MeterRegistry meterRegistry;
    private final ApplicationContext applicationContext;

    private volatile boolean running = true;

    public RedisStreamLagMetricService(
            StringRedisTemplate stringRedisTemplate,
            RedisStreamProperties redisStreamProperties,
            MeterRegistry meterRegistry,
            ApplicationContext applicationContext
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisStreamProperties = redisStreamProperties;
        this.meterRegistry = meterRegistry;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void initializeMetrics() {
        if (redisStreamProperties.keys() == null) {
            log.warn("Redis Stream 설정이 없습니다. Lag 메트릭을 등록하지 않습니다.");
            return;
        }

        for (Map.Entry<String, StreamConfig> entry : redisStreamProperties.keys().entrySet()) {
            final String streamKey = entry.getKey();
            final StreamConfig streamConfig = entry.getValue();

            // 1) 스트림 길이 Gauge (XLEN - O(1))
            Gauge.builder("redis.stream.length", () -> getStreamLength(streamKey))
                    .description("Redis Stream의 현재 메시지 수 (XLEN)")
                    .tag("stream", streamKey)
                    .register(meterRegistry);

            // 리스너 미생성 스트림(컨슈머 그룹 전용)은 소비 스레드풀이 없다 — 길이 게이지만 등록한다.
            // 그룹 lag·pending은 XINFO GROUPS 기반 전용 메트릭이 담당한다(#1610).
            if (!streamConfig.isListenerEnabled()) {
                continue;
            }

            // 2) 스레드풀 큐 깊이 Gauge
            Gauge.builder("redis.stream.threadpool.queue.size",
                            () -> getThreadPoolQueueSize(streamKey, streamConfig))
                    .description("Redis Stream 컨슈머 스레드풀의 대기 큐 크기")
                    .tag("stream", streamKey)
                    .register(meterRegistry);

            // 3) 스레드풀 활성 스레드 수
            Gauge.builder("redis.stream.threadpool.active.count",
                            () -> getThreadPoolActiveCount(streamKey, streamConfig))
                    .description("Redis Stream 컨슈머 스레드풀의 활성 스레드 수")
                    .tag("stream", streamKey)
                    .register(meterRegistry);
        }

        log.info("Redis Stream Lag 메트릭 등록 완료: streams={}",
                redisStreamProperties.keys().keySet());
    }

    @Override
    public void start() {
        running = true;
    }

    /**
     * 컨텍스트 종료 시작 신호. 이후 Redis 조회 게이지는 호출 자체를 건너뛴다.
     */
    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    // 폴러가 멈춘 뒤에도 백로그는 관측 대상이므로 폴러 정지보다 뒤, 조회가 불가능해지는
    // 커넥션 팩토리 정지보다는 앞에 선다. 전체 순서표는 docs/architecture.md "종료 순서 (lifecycle phase)"
    @Override
    public int getPhase() {
        return 512;
    }

    private double getStreamLength(String streamKey) {
        if (!running) {
            return Double.NaN;
        }
        try {
            Long length = stringRedisTemplate.opsForStream().size(streamKey);
            return length != null ? length.doubleValue() : 0.0;
        } catch (Exception e) {
            log.warn("XLEN 조회 실패: stream={}", streamKey, e);
            return Double.NaN;
        }
    }

    private double getThreadPoolQueueSize(String streamKey, StreamConfig config) {
        try {
            ThreadPoolTaskExecutor executor = resolveExecutor(streamKey, config);
            if (executor == null) {
                return Double.NaN;
            }

            ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
            return threadPoolExecutor.getQueue().size();
        } catch (Exception e) {
            log.debug("스레드풀 큐 크기 조회 실패: stream={}", streamKey, e);
            return Double.NaN;
        }
    }

    private double getThreadPoolActiveCount(String streamKey, StreamConfig config) {
        try {
            ThreadPoolTaskExecutor executor = resolveExecutor(streamKey, config);
            if (executor == null) {
                return Double.NaN;
            }

            return executor.getThreadPoolExecutor().getActiveCount();
        } catch (Exception e) {
            log.debug("활성 스레드 수 조회 실패: stream={}", streamKey, e);
            return Double.NaN;
        }
    }

    private ThreadPoolTaskExecutor resolveExecutor(String streamKey, StreamConfig config) {
        try {
            String beanName = resolveBeanName(streamKey, config);
            Object bean = applicationContext.getBean(beanName);

            if (!(bean instanceof ThreadPoolTaskExecutor executor)) {
                return null;
            }

            return executor;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveBeanName(String streamKey, StreamConfig config) {
        if (config != null && config.threadPoolName() != null) {
            return RedisStreamThreadPoolConfig.convertBeanName(config.threadPoolName());
        }

        return RedisStreamThreadPoolConfig.convertBeanName(streamKey);
    }
}
