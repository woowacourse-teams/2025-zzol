package coffeeshout.config;

import coffeeshout.user.application.port.ReportAnonymizationPort;
import java.time.Clock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
@Import(GameSchedulerTestConfig.class)
public class ServiceTestConfig {

    @Bean
    @Primary
    public SimpMessagingTemplate mockMessagingTemplate() {
        return Mockito.mock(SimpMessagingTemplate.class);
    }

    @Bean
    @Primary
    public Clock testClock() {
        return Clock.systemUTC();
    }

    // :user의 UserWithdrawalService가 ReportAnonymizationPort에 의존하지만 실구현체(:admin)는 클래스패스 밖이므로 mock 등록
    @Bean
    @Primary
    public ReportAnonymizationPort mockReportAnonymizationPort() {
        return Mockito.mock(ReportAnonymizationPort.class);
    }
}
