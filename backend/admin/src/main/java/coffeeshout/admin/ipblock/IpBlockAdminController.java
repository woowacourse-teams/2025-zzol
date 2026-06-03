package coffeeshout.admin.ipblock;

import coffeeshout.global.ipblock.IpBlockStore.BlockedIp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/ip-blocks")
@RequiredArgsConstructor
public class IpBlockAdminController {

    private final IpBlockAdminService ipBlockAdminService;

    @GetMapping
    public String list(Model model) {
        final List<BlockedIp> blockedIps = ipBlockAdminService.getBlockedIps();
        model.addAttribute("blockedIps", blockedIps);
        return "admin/ip-blocks";
    }

    @PostMapping("/{ip}/unblock")
    public String unblock(@PathVariable String ip) {
        ipBlockAdminService.unblock(ip);
        return "redirect:/admin/ip-blocks";
    }
}
