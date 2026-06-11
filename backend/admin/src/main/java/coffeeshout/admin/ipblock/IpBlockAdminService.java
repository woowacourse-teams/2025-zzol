package coffeeshout.admin.ipblock;

import coffeeshout.global.ipblock.Ip;
import coffeeshout.global.ipblock.IpBlockStore;
import coffeeshout.global.ipblock.IpBlockStore.BlockedIp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IpBlockAdminService {

    private final IpBlockStore ipBlockStore;

    public List<BlockedIp> getBlockedIps() {
        return ipBlockStore.getBlockedIps();
    }

    public void unblock(Ip ip) {
        ipBlockStore.unblock(ip);
    }
}
