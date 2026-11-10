package coffeeshout.config;

import coffeeshout.friend.application.port.RoomInvitationValidator;
import coffeeshout.friend.application.port.RoomMembershipQuery;
import coffeeshout.gamecommon.MemberMiniGameRecordQuery;
import coffeeshout.gamecommon.MemberRouletteRecordQuery;
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
    @Primary
    public ProfanityChecker mockProfanityChecker() {
        return Mockito.mock(ProfanityChecker.class);
    }

    // 내 기록 포트(#1794)의 실구현체는 :room·:game에 있어 :user 컨텍스트에는 없다
    @Bean
    @Primary
    public MemberRouletteRecordQuery mockMemberRouletteRecordQuery() {
        return Mockito.mock(MemberRouletteRecordQuery.class);
    }

    @Bean
    @Primary
    public MemberMiniGameRecordQuery mockMemberMiniGameRecordQuery() {
        return Mockito.mock(MemberMiniGameRecordQuery.class);
    }

    @Bean("stompPrincipalInterceptor")
    @Primary
    public ChannelInterceptor mockStompPrincipalInterceptor() {
        return Mockito.mock(ChannelInterceptor.class);
    }

    @Bean
    @Primary
    public RoomInvitationValidator mockRoomInvitationValidator() {
        return Mockito.mock(RoomInvitationValidator.class);
    }

    @Bean
    @Primary
    public RoomMembershipQuery mockRoomMembershipQuery() {
        return Mockito.mock(RoomMembershipQuery.class);
    }

    @Bean
    @Primary
    public ReportAnonymizationPort mockReportAnonymizationPort() {
        return Mockito.mock(ReportAnonymizationPort.class);
    }

    @Bean
    @Primary
    public WordPicker mockWordPicker() {
        return Mockito.mock(WordPicker.class);
    }
}
