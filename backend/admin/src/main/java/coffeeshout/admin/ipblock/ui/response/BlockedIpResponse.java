package coffeeshout.admin.ipblock.ui.response;

import coffeeshout.global.ipblock.IpBlockStore.BlockedIp;

public record BlockedIpResponse(String ip, long remainingTtlSeconds) {

    public static BlockedIpResponse from(BlockedIp blockedIp) {
        return new BlockedIpResponse(blockedIp.ip(), blockedIp.remainingTtlSeconds());
    }
}
