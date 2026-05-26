package coffeeshout.admin.profanity.ui;

import coffeeshout.admin.profanity.ui.request.AddProfanityWordRequest;
import coffeeshout.profanity.application.ProfanityWordManagementService;
import coffeeshout.profanity.domain.Language;
import coffeeshout.profanity.domain.WordSource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/profanity/words")
@RequiredArgsConstructor
public class ProfanityWordAdminController {

    private final ProfanityWordManagementService managementService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("words", managementService.findAllActive());
        model.addAttribute("languages", Language.values());
        return "admin/profanity-words";
    }

    @PostMapping
    public String add(@Valid @ModelAttribute AddProfanityWordRequest request,
                      BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("words", managementService.findAllActive());
            model.addAttribute("languages", Language.values());
            return "admin/profanity-words";
        }
        managementService.add(request.word(), request.language(), WordSource.MANUAL);
        return "redirect:/admin/profanity/words";
    }

    @PostMapping("/deactivate")
    public String deactivate(@RequestParam String word) {
        managementService.deactivate(word);
        return "redirect:/admin/profanity/words";
    }
}
