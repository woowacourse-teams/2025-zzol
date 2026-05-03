package coffeeshout.patchnote.ui;

import coffeeshout.patchnote.application.PatchNoteAdminService;
import coffeeshout.patchnote.application.PatchNoteAdminService.AdminRow;
import coffeeshout.patchnote.domain.PatchNoteCategory;
import coffeeshout.patchnote.ui.request.CreatePatchNoteRequest;
import coffeeshout.patchnote.ui.request.UpdatePatchNoteRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/patch-notes")
@RequiredArgsConstructor
public class PatchNoteAdminController {

    private final PatchNoteAdminService patchNoteAdminService;

    @GetMapping
    public String list(Model model) {
        final List<AdminRow> patchNotes = patchNoteAdminService.findAll();
        model.addAttribute("patchNotes", patchNotes);
        model.addAttribute("totalCount", patchNotes.size());
        return "admin/patch-note-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categories", PatchNoteCategory.values());
        model.addAttribute("formAction", "/admin/patch-notes");
        model.addAttribute("patchNote", null);
        return "admin/patch-note-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute CreatePatchNoteRequest request) {
        patchNoteAdminService.create(request.category(), request.title(), request.content());
        return "redirect:/admin/patch-notes";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        final AdminRow patchNote = patchNoteAdminService.findById(id);
        model.addAttribute("categories", PatchNoteCategory.values());
        model.addAttribute("formAction", "/admin/patch-notes/" + id);
        model.addAttribute("patchNote", patchNote);
        return "admin/patch-note-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute UpdatePatchNoteRequest request) {
        patchNoteAdminService.update(id, request.category(), request.title(), request.content());
        return "redirect:/admin/patch-notes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        patchNoteAdminService.delete(id);
        return "redirect:/admin/patch-notes";
    }
}
