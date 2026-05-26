package coffeeshout.admin.room.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/playername-audit")
public class PlayerNameAuditAdminController {

    @GetMapping
    public String redirect(
            @RequestParam(defaultValue = "0") int flaggedPage,
            @RequestParam(defaultValue = "0") int pendingPage) {
        return "redirect:/admin/profanity?tab=audit&flaggedPage=" + flaggedPage + "&pendingPage=" + pendingPage;
    }
}
