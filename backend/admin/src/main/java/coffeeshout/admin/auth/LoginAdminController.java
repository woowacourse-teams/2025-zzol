package coffeeshout.admin.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginAdminController {

    @GetMapping({"/admin", "/admin/"})
    public String index() {
        return "admin/index";
    }

    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }
}
