package design.hitsuji.otetsudai.controller;

import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.service.AllowanceService;
import design.hitsuji.otetsudai.service.ChoreService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/requests")
public class RequestController {

    private final ChoreService choreService;
    private final AllowanceService allowanceService;

    public RequestController(ChoreService choreService, AllowanceService allowanceService) {
        this.choreService = choreService;
        this.allowanceService = allowanceService;
    }

    @GetMapping("/new")
    public String newRequest(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Chore> unpaidChores = choreService.listUnpaidChores(userDetails.getUsername());
        model.addAttribute("unpaidChores", unpaidChores);
        model.addAttribute("hasUnpaid", !unpaidChores.isEmpty());
        return "request/new";
    }

    @PostMapping("/preview")
    @ResponseBody
    public Map<String, Integer> preview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) List<Long> choreIds) {
        try {
            int amount = allowanceService.previewAmount(userDetails.getUsername(), choreIds);
            return Map.of("amount", amount);
        } catch (IllegalStateException e) {
            return Map.of("amount", 0);
        }
    }

    @PostMapping
    public String createRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) List<Long> choreIds,
            RedirectAttributes redirectAttributes) {
        try {
            allowanceService.createRequest(userDetails.getUsername(), choreIds);
            return "redirect:/chores?applied=true";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/requests/new";
        }
    }
}
