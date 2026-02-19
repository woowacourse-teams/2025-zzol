package coffeeshout.global.health;

import static coffeeshout.global.redis.config.RedisStreamListenerStarter.STREAM_CONTAINER_BEAN_NAME_FORMAT;

import coffeeshout.global.redis.config.RedisStreamProperties;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis Stream ListenerContainer의 상태를 주기적으로 감시하고,
 * 멈춘 container를 자동으로 재시작하는 복구 컴포넌트.
 * <p>
 * HealthIndicator에서 직접 복구하지 않는 이유:
 * HealthIndicator는 "상태를 보고하는 역할"이지 "상태를 고치는 역할"이 아니다.
 * 헬스체크 호출 시 부수효과(side effect)가 발생하면 책임이 섞인다.
 * <p>
 * 복구 흐름:
 * 1. 30초마다 모든 container 상태를 체크
 * 2. 멈춘 container 발견 시 start()를 호출하여 복구 시도
 * 3. 복구 실패 시 failedRecoveryStreams에 기록
 * 4. HealthIndicator는 이 정보를 읽어서, 복구 실패한 container가 있으면 DOWN 반환
 * 5. DOWN이 되면 Docker HEALTHCHECK에 의해 컨테이너 재시작 (last resort)
 * <p>
 * 스트림 키 목록은 RedisStreamProperties에서 동적으로 로드한다.
 * application.yml에 스트림을 추가/제거하면 자동으로 반영된다.
 */
@Slf4j
@Component
public class RedisStreamContainerRecovery {

    private final ApplicationContext applicationContext;
    private final RedisStreamProperties redisStreamProperties;

    /**
     * 복구 시도했지만 실패한 스트림 키 목록.
     * HealthIndicator가 이 Set을 참조하여 DOWN 여부를 결정한다.
     * <p>
     * ConcurrentHashMap 기반: @Scheduled 스케줄러 스레드(쓰기)와
     * /actuator/health HTTP 스레드(읽기)가 동시에 접근하므로 thread-safe 자료구조 사용.
     */
    private final Set<String> failedRecoveryStreams = ConcurrentHashMap.newKeySet();

    /**
     * 각 스트림별 연속 복구 실패 횟수.
     * 1회 실패 시에는 재시도, 2회 연속 실패 시 failedRecoveryStreams에 등록.
     */
    private final Map<String, Integer> recoveryFailureCounts = new ConcurrentHashMap<>();

    private static final int MAX_RECOVERY_ATTEMPTS = 2;

    public RedisStreamContainerRecovery(ApplicationContext applicationContext,
                                        RedisStreamProperties redisStreamProperties) {
        this.applicationContext = applicationContext;
        this.redisStreamProperties = redisStreamProperties;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 60_000)
    public void checkAndRecover() {
        if (redisStreamProperties.keys() == null) {
            return;
        }

        for (String streamKey : redisStreamProperties.keys().keySet()) {
            final String beanName = String.format(STREAM_CONTAINER_BEAN_NAME_FORMAT, streamKey);

            try {
                final StreamMessageListenerContainer<?, ?> container =
                        applicationContext.getBean(beanName, StreamMessageListenerContainer.class);

                if (container.isRunning()) {
                    recoveryFailureCounts.remove(streamKey);
                    failedRecoveryStreams.remove(streamKey);
                    continue;
                }

                log.warn("Redis Stream container 중단 감지, 복구 시도: stream={}", streamKey);
                attemptRecovery(streamKey, container);

            } catch (NoSuchBeanDefinitionException e) {
                log.debug("Redis Stream container 빈 없음: stream={}", streamKey);
            }
        }
    }

    private void attemptRecovery(String streamKey, StreamMessageListenerContainer<?, ?> container) {
        try {
            container.start();

            if (container.isRunning()) {
                log.info("Redis Stream container 복구 성공: stream={}", streamKey);
                recoveryFailureCounts.remove(streamKey);
                failedRecoveryStreams.remove(streamKey);
            } else {
                handleRecoveryFailure(streamKey);
            }
        } catch (Exception e) {
            log.error("Redis Stream container 복구 중 예외 발생: stream={}", streamKey, e);
            handleRecoveryFailure(streamKey);
        }
    }

    private void handleRecoveryFailure(String streamKey) {
        int failCount = recoveryFailureCounts.merge(streamKey, 1, Integer::sum);
        log.warn("Redis Stream container 복구 실패: stream={}, 연속 실패 횟수={}/{}",
                streamKey, failCount, MAX_RECOVERY_ATTEMPTS);

        if (failCount >= MAX_RECOVERY_ATTEMPTS) {
            failedRecoveryStreams.add(streamKey);
            log.error("Redis Stream container 복구 포기: stream={}. HealthIndicator에 DOWN 보고", streamKey);
        }
    }

    public boolean hasUnrecoverableStreams() {
        return !failedRecoveryStreams.isEmpty();
    }

    public Set<String> getFailedRecoveryStreams() {
        return Set.copyOf(failedRecoveryStreams);
    }
}
