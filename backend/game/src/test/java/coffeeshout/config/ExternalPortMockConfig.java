package coffeeshout.config;

import coffeeshout.user.application.port.ReportAnonymizationPort;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 구현체가 게임 모듈 테스트 클래스패스 밖에 있는 포트의 mock 모음.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ExternalPortMockConfig {

    // :user의 UserWithdrawalService가 ReportAnonymizationPort에 의존하지만 실구현체(:admin)는 클래스패스 밖이므로 mock 등록
    @Bean
    @Primary
    public ReportAnonymizationPort mockReportAnonymizationPort() {
        return Mockito.mock(ReportAnonymizationPort.class);
    }
}
