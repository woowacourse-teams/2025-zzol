package coffeeshout.config;

import coffeeshout.friend.application.port.RoomInvitationValidator;
import coffeeshout.global.nickname.ProfanityChecker;
import coffeeshout.global.nickname.WordPicker;
import coffeeshout.user.application.port.ReportAnonymizationPort;
import java.time.Clock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.ChannelInterceptor;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class ServiceTestConfig {

    @Bean
    @Primary
    public Clock testClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ProfanityChecker mockProfanityChecker() {
        return Mockito.mock(ProfanityChecker.class);
    }

    @Bean("stompPrincipalInterceptor")
    public ChannelInterceptor mockStompPrincipalInterceptor() {
        return Mockito.mock(ChannelInterceptor.class);
    }

    @Bean
    public RoomInvitationValidator mockRoomInvitationValidator() {
        return Mockito.mock(RoomInvitationValidator.class);
    }

    @Bean
    public ReportAnonymizationPort mockReportAnonymizationPort() {
        return Mockito.mock(ReportAnonymizationPort.class);
    }

    @Bean
    public WordPicker mockWordPicker() {
        return Mockito.mock(WordPicker.class);
    }
}
