package coffeeshout.admin.profanity.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/profanity/words")
public class ProfanityWordAdminController {

    @GetMapping
    public String redirect() {
        return "redirect:/admin/profanity?tab=words";
    }
}
