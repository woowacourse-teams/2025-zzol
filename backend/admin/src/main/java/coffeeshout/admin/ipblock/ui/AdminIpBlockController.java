package coffeeshout.admin.ipblock.ui;

import coffeeshout.admin.ipblock.IpBlockAdminService;
import coffeeshout.admin.ipblock.ui.response.BlockedIpResponse;
import coffeeshout.global.ipblock.Ip;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * IP 차단 관리. 기존 Thymeleaf {@code IpBlockAdminController}의 REST 판이다.
 *
 * <p>해제는 POST 가 아니라 DELETE 다. 차단이라는 자원을 없애는 것이므로 그쪽이 맞고,
 * 폼 제출이 아니라 fetch 로 부르는 SPA 에서는 메서드 제약도 없다.
 */
@RestController
@RequestMapping("/admin/api/ip-blocks")
@RequiredArgsConstructor
public class AdminIpBlockController {

    private final IpBlockAdminService ipBlockAdminService;

    @GetMapping
    public List<BlockedIpResponse> list() {
        return ipBlockAdminService.getBlockedIps().stream()
                .map(BlockedIpResponse::from)
                .toList();
    }

    @DeleteMapping("/{ip}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable String ip) {
        // Ip 생성자가 형식을 검증한다. 검증 없이 넘기면 block:ip:* 같은 값으로
        // 레디스 키 패턴을 건드리는 경로가 열린다.
        ipBlockAdminService.unblock(new Ip(ip));
    }
}
