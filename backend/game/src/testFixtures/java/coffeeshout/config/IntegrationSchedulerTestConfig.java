package coffeeshout.config;

import coffeeshout.game.flow.CompletableFutureFlowScheduler;
import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.support.ShutDownTestScheduler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;

/**
 * 통합테스트용 게임 스케줄러 미러 — 프로덕션의 {@code @Profile("!test")} 스케줄러 빈을 같은 이름으로 대체한다.
 *
 * <p>game IT와 app IT가 이 한 곳을 함께 import한다. 예전에는 같은 내용이 두 모듈에 복사돼 있었고,
 * 한쪽만 고쳐 나머지 모듈의 IT가 통째로 깨지는 사고가 반복됐다(postmortem 0004, PR #1484에서 55건).
 * 정의를 한 곳으로 모아 그 실패 모드 자체를 없앤다.
 */
@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class IntegrationSchedulerTestConfig {

    @Bean(name = "cardGameExecutorScheduler")
    public ShutDownTestScheduler cardGameExecutorScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "cardGameFlowScheduler")
    public FlowScheduler cardGameFlowScheduler(ShutDownTestScheduler cardGameExecutorScheduler) {
        return new CompletableFutureFlowScheduler(cardGameExecutorScheduler);
    }

    @Bean(name = "blockStackingExecutorScheduler")
    public ShutDownTestScheduler blockStackingExecutorScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "blockStackingFlowScheduler")
    public FlowScheduler blockStackingFlowScheduler(ShutDownTestScheduler blockStackingExecutorScheduler) {
        return new CompletableFutureFlowScheduler(blockStackingExecutorScheduler);
    }

    @Bean(name = "ladderExecutorScheduler")
    public ShutDownTestScheduler ladderExecutorScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "ladderFlowScheduler")
    public FlowScheduler ladderFlowScheduler(ShutDownTestScheduler ladderExecutorScheduler) {
        return new CompletableFutureFlowScheduler(ladderExecutorScheduler);
    }

    @Bean(name = "racingGameScheduler")
    public TaskScheduler testIntegrationRacingGameScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "speedTouchGameScheduler")
    public TaskScheduler testIntegrationSpeedTouchGameScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "blindTimerGameScheduler")
    public TaskScheduler testIntegrationBlindTimerGameScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "nunchiGameScheduler")
    public TaskScheduler testIntegrationNunchiGameScheduler() {
        return new ShutDownTestScheduler();
    }
}
