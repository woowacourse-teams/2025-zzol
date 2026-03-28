package coffeeshout.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginAdminController {

    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }
}
