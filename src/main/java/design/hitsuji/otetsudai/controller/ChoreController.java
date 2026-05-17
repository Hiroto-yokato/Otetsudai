package design.hitsuji.otetsudai.controller;

import design.hitsuji.otetsudai.dto.ChoreForm;
import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import design.hitsuji.otetsudai.service.ChoreService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/chores")
public class ChoreController {

    private final ChoreService choreService;

    public ChoreController(ChoreService choreService) {
        this.choreService = choreService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Chore> chores = choreService.listChores(userDetails.getUsername());
        model.addAttribute("chores", chores);
        model.addAttribute("hasUnpaid", chores.stream().anyMatch(c -> c.getStatus() == ChoreStatus.UNPAID));
        return "chore/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        ChoreForm form = new ChoreForm();
        form.setChoreDate(LocalDate.now());
        model.addAttribute("choreForm", form);
        return "chore/new";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute ChoreForm choreForm,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            return "chore/new";
        }
        choreService.createChore(userDetails.getUsername(), choreForm.getChoreDate(), choreForm.getContent());
        return "redirect:/chores?registered=true";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            choreService.deleteChore(id, userDetails.getUsername());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/chores";
    }
}
