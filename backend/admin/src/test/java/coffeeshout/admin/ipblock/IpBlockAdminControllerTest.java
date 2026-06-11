package coffeeshout.admin.ipblock;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import coffeeshout.global.exception.GlobalErrorCode;
import coffeeshout.global.ipblock.Ip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("IpBlockAdminController")
@ExtendWith(MockitoExtension.class)
class IpBlockAdminControllerTest {

    @Mock
    private IpBlockAdminService ipBlockAdminService;

    @InjectMocks
    private IpBlockAdminController ipBlockAdminController;

    @Nested
    class unblock {

        @Test
        void 유효한_IP면_Ip_객체로_변환해_차단을_해제하고_목록으로_리다이렉트한다() {
            final String view = ipBlockAdminController.unblock("1.2.3.4");

            assertThat(view).isEqualTo("redirect:/admin/ip-blocks");
            then(ipBlockAdminService).should().unblock(new Ip("1.2.3.4"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-an-ip", "256.1.1.1", "block:ip:*"})
        void 유효하지_않은_IP_형식이면_예외를_던지고_서비스를_호출하지_않는다(String invalidIp) {
            assertCoffeeShoutException(
                    () -> ipBlockAdminController.unblock(invalidIp),
                    GlobalErrorCode.VALIDATION_ERROR
            );

            then(ipBlockAdminService).shouldHaveNoInteractions();
        }
    }
}
