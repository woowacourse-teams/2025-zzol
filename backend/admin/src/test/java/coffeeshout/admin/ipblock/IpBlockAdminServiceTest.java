package coffeeshout.admin.ipblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import coffeeshout.global.ipblock.Ip;
import coffeeshout.global.ipblock.IpBlockStore;
import coffeeshout.global.ipblock.IpBlockStore.BlockedIp;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("IpBlockAdminService")
@ExtendWith(MockitoExtension.class)
class IpBlockAdminServiceTest {

    @Mock
    private IpBlockStore ipBlockStore;

    @InjectMocks
    private IpBlockAdminService ipBlockAdminService;

    @Nested
    class unblock {

        @Test
        void store에_차단_해제를_위임한다() {
            final Ip ip = new Ip("1.2.3.4");

            ipBlockAdminService.unblock(ip);

            then(ipBlockStore).should().unblock(ip);
        }
    }

    @Nested
    class getBlockedIps {

        @Test
        void store의_차단_IP_목록을_그대로_반환한다() {
            final List<BlockedIp> blockedIps = List.of(new BlockedIp("1.2.3.4", 3600L));
            given(ipBlockStore.getBlockedIps()).willReturn(blockedIps);

            final List<BlockedIp> result = ipBlockAdminService.getBlockedIps();

            assertThat(result).isEqualTo(blockedIps);
        }
    }
}
