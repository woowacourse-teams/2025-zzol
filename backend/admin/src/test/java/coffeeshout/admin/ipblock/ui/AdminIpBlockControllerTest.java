package coffeeshout.admin.ipblock.ui;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import coffeeshout.admin.ipblock.IpBlockAdminService;
import coffeeshout.admin.ipblock.ui.response.BlockedIpResponse;
import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.ipblock.Ip;
import coffeeshout.global.ipblock.IpBlockStore.BlockedIp;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AdminIpBlockController")
@ExtendWith(MockitoExtension.class)
class AdminIpBlockControllerTest {

    @Mock
    private IpBlockAdminService ipBlockAdminService;

    @InjectMocks
    private AdminIpBlockController adminIpBlockController;

    @Nested
    class list {

        @Test
        void 차단된_IP와_잔여_TTL을_돌려준다() {
            given(ipBlockAdminService.getBlockedIps()).willReturn(List.of(new BlockedIp("1.2.3.4", 3600)));

            assertThat(adminIpBlockController.list()).containsExactly(new BlockedIpResponse("1.2.3.4", 3600));
        }

        @Test
        void 차단이_없으면_빈_목록을_돌려준다() {
            given(ipBlockAdminService.getBlockedIps()).willReturn(List.of());

            assertThat(adminIpBlockController.list()).isEmpty();
        }
    }

    @Nested
    class unblock {

        @Test
        void 유효한_IP면_Ip_객체로_변환해_해제한다() {
            adminIpBlockController.unblock("1.2.3.4");

            then(ipBlockAdminService).should().unblock(new Ip("1.2.3.4"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-an-ip", "256.1.1.1", "block:ip:*"})
        void 형식이_아니면_서비스를_호출하지_않는다(String invalidIp) {
            // 검증 없이 넘기면 레디스 키 패턴을 건드리는 경로가 열린다.
            assertCoffeeShoutException(
                    () -> adminIpBlockController.unblock(invalidIp), GlobalErrorCode.VALIDATION_ERROR);

            then(ipBlockAdminService).shouldHaveNoInteractions();
        }
    }
}
