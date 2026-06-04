package coffeeshout.game.scheduler;

import io.micrometer.context.ContextSnapshotFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

/**
 * 게임 스케줄러 공통 정책을 한곳에서 보장하는 팩토리.
 * <p>
 * 모든 게임 스케줄러는 이 팩토리로 생성한다. 개별 설정에서 직접 {@code new ThreadPoolTaskScheduler()}를
 * 만들면 아래 정책이 누락될 수 있다:
 * <ul>
 *   <li>제출 시점 트레이스 컨텍스트 전파 — 지연 실행 후 Stream 발행 시 trace가 끊기지 않는다</li>
 *   <li>graceful shutdown — 진행 중 게임 태스크 완료 대기 (최대 30초)</li>
 *   <li>스케줄 실행 예외 로깅</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class GameTaskSchedulerFactory {

    private static final int AWAIT_TERMINATION_SECONDS = 30;

    private final ContextSnapshotFactory snapshotFactory;

    public ThreadPoolTaskScheduler create(String threadNamePrefix, int poolSize, String errorLogMessage) {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(1, poolSize));
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.setDaemon(false);
        // 지연 실행 후 Stream 발행 시 trace가 끊기지 않도록 제출 시점 컨텍스트를 전파한다
        scheduler.setTaskDecorator(runnable -> snapshotFactory.captureAll().wrap(runnable));
        scheduler.setErrorHandler(t -> log.error(errorLogMessage, t));
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        scheduler.initialize();
        return scheduler;
    }
}
