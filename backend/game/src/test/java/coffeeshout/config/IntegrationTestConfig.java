package coffeeshout.config;

import coffeeshout.game.flow.CompletableFutureFlowScheduler;
import coffeeshout.gamecommon.flow.FlowScheduler;
import coffeeshout.support.ShutDownTestScheduler;
import coffeeshout.user.application.port.ReportAnonymizationPort;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class IntegrationTestConfig {

    @Bean(name = "cardGameExecutorScheduler")
    public ShutDownTestScheduler cardGameExecutorScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "cardGameFlowScheduler")
    public FlowScheduler cardGameFlowScheduler(
            ShutDownTestScheduler cardGameExecutorScheduler) {
        return new CompletableFutureFlowScheduler(cardGameExecutorScheduler);
    }

    @Bean(name = "blockStackingExecutorScheduler")
    public ShutDownTestScheduler blockStackingExecutorScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "blockStackingFlowScheduler")
    public FlowScheduler blockStackingFlowScheduler(
            ShutDownTestScheduler blockStackingExecutorScheduler) {
        return new CompletableFutureFlowScheduler(blockStackingExecutorScheduler);
    }

    @Bean(name = "ladderExecutorScheduler")
    public ShutDownTestScheduler ladderExecutorScheduler() {
        return new ShutDownTestScheduler();
    }

    @Bean(name = "ladderFlowScheduler")
    public FlowScheduler ladderFlowScheduler(
            ShutDownTestScheduler ladderExecutorScheduler) {
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

    // :user의 UserWithdrawalService가 ReportAnonymizationPort에 의존하지만 실구현체(:admin)는 클래스패스 밖이므로 mock 등록
    @Bean
    @Primary
    public ReportAnonymizationPort mockReportAnonymizationPort() {
        return Mockito.mock(ReportAnonymizationPort.class);
    }
}
