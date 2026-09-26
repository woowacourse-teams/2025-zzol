package coffeeshout.config;

import coffeeshout.gamecommon.MemberMiniGameRecordQuery;
import coffeeshout.user.application.port.ReportAnonymizationPort;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class ServiceTestConfig {

    // :room은 :user의 ReportAnonymizationPort에 의존하지만 test 프로파일에선 실구현체가 없으므로 mock으로 대체
    @Bean
    @Primary
    public ReportAnonymizationPort mockReportAnonymizationPort() {
        return Mockito.mock(ReportAnonymizationPort.class);
    }

    // :user의 MemberRecordService(#1794)가 :game의 MemberMiniGameRecordQuery를 요구하지만 :room 컨텍스트에는 없다
    @Bean
    @Primary
    public MemberMiniGameRecordQuery mockMemberMiniGameRecordQuery() {
        return Mockito.mock(MemberMiniGameRecordQuery.class);
    }
}
