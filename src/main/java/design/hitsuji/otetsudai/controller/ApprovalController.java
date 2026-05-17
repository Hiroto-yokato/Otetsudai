package design.hitsuji.otetsudai.controller;

import design.hitsuji.otetsudai.entity.AllowanceRequest;
import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.service.AllowanceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ApprovalController {

    private final AllowanceService allowanceService;

    public ApprovalController(AllowanceService allowanceService) {
        this.allowanceService = allowanceService;
    }

    @GetMapping("/approvals")
    public String list(Model model) {
        List<AllowanceRequest> pendingRequests = allowanceService.listPendingRequests();
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("childNames", buildChildNameMap(pendingRequests));
        return "approvals/list";
    }

    @GetMapping("/approvals/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            AllowanceRequest request = allowanceService.getRequest(id);
            List<Chore> chores = allowanceService.getChoresForRequest(id);
            model.addAttribute("request", request);
            model.addAttribute("chores", chores);
            model.addAttribute("childName", allowanceService.getChildLoginId(request.getChildUserId()));
            return "approvals/detail";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/approvals";
        }
    }

    @PostMapping("/approvals/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            allowanceService.approveRequest(id);
            redirectAttributes.addFlashAttribute("approvedMessage", "申請を承認しました");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/approvals";
    }

    @PostMapping("/approvals/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            allowanceService.rejectRequest(id, reason);
            redirectAttributes.addFlashAttribute("rejectedMessage", "申請を却下しました");
            return "redirect:/approvals";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/approvals/" + id;
        }
    }

    @GetMapping("/history")
    public String history(Model model) {
        List<AllowanceRequest> approvedRequests = allowanceService.listApprovedHistory();
        model.addAttribute("approvedRequests", approvedRequests);
        model.addAttribute("childNames", buildChildNameMap(approvedRequests));
        return "approvals/history";
    }

    private Map<Long, String> buildChildNameMap(List<AllowanceRequest> requests) {
        Map<Long, String> map = new LinkedHashMap<>();
        for (AllowanceRequest req : requests) {
            map.computeIfAbsent(req.getChildUserId(), allowanceService::getChildLoginId);
        }
        return map;
    }
}
