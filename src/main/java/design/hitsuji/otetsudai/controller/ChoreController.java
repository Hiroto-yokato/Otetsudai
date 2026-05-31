package design.hitsuji.otetsudai.controller;

import design.hitsuji.otetsudai.dto.BulkChoreForm;
import design.hitsuji.otetsudai.dto.ChoreForm;
import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.entity.User;
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
import java.util.Map;

/**
 * お手伝いのCRUDと一括登録・家族一覧を担うコントローラー（{@code /chores}）。
 *
 * <p>{@link #CHORE_TEMPLATES} は入力補助用の定型文リストで、
 * 全フォームの {@code <datalist>} に渡す。
 */
@Controller
@RequestMapping("/chores")
public class ChoreController {

    /** お手伝い入力補助用の定型文リスト。 */
    private static final List<String> CHORE_TEMPLATES = List.of(
            "掃除機をかける",
            "トイレ掃除",
            "お風呂掃除",
            "食器を洗う",
            "食器を片付ける",
            "洗濯物をたたむ",
            "洗濯物を干す",
            "料理のお手伝い",
            "ゴミを捨てる",
            "テーブルを拭く"
    );

    private final ChoreService choreService;

    public ChoreController(ChoreService choreService) {
        this.choreService = choreService;
    }

    /** お手伝い一覧を表示する（子ども向け）。 */
    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Chore> chores = choreService.listChores(userDetails.getUsername());
        model.addAttribute("chores", chores);
        model.addAttribute("hasUnpaid", chores.stream().anyMatch(c -> c.getStatus() == ChoreStatus.UNPAID));
        return "chore/list";
    }

    /** 家族全員の子どものお手伝い一覧を表示する（親向け）。 */
    @GetMapping("/family")
    public String familyList(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Chore> chores = choreService.listFamilyChores(userDetails.getUsername());
        List<Long> userIds = chores.stream().map(Chore::getUserId).distinct().toList();
        Map<Long, User> userMap = choreService.buildUserMap(userIds);
        model.addAttribute("chores", chores);
        model.addAttribute("userMap", userMap);
        return "chore/family";
    }

    /** 一括登録フォームを表示する（初期値: 5行・今日の日付）。 */
    @GetMapping("/bulk")
    public String bulkForm(Model model) {
        BulkChoreForm form = new BulkChoreForm();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 5; i++) {
            BulkChoreForm.ChoreEntryForm entry = new BulkChoreForm.ChoreEntryForm();
            entry.setChoreDate(today);
            form.getEntries().add(entry);
        }
        model.addAttribute("bulkForm", form);
        model.addAttribute("choreTemplates", CHORE_TEMPLATES);
        return "chore/bulk";
    }

    /**
     * 一括登録を実行する。
     * 成功時は {@code /chores?registered=true} にリダイレクトする。
     */
    @PostMapping("/bulk")
    public String createBulk(@AuthenticationPrincipal UserDetails userDetails,
                             @ModelAttribute("bulkForm") BulkChoreForm bulkForm,
                             Model model) {
        try {
            choreService.createChoresBulk(userDetails.getUsername(), bulkForm);
            return "redirect:/chores?registered=true";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("choreTemplates", CHORE_TEMPLATES);
            return "chore/bulk";
        }
    }

    /** 単体登録フォームを表示する（初期値: 今日の日付）。 */
    @GetMapping("/new")
    public String newForm(Model model) {
        ChoreForm form = new ChoreForm();
        form.setChoreDate(LocalDate.now());
        model.addAttribute("choreForm", form);
        model.addAttribute("choreTemplates", CHORE_TEMPLATES);
        return "chore/new";
    }

    /**
     * お手伝いを1件登録する。
     * 成功時は {@code /chores?registered=true} にリダイレクトする。
     */
    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute ChoreForm choreForm,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("choreTemplates", CHORE_TEMPLATES);
            return "chore/new";
        }
        choreService.createChore(userDetails.getUsername(), choreForm.getChoreDate(), choreForm.getContent());
        return "redirect:/chores?registered=true";
    }

    /** お手伝い編集フォームを表示する。 */
    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            Chore chore = choreService.getChoreForEdit(id, userDetails.getUsername());
            ChoreForm form = new ChoreForm();
            form.setChoreDate(chore.getChoreDate());
            form.setContent(chore.getContent());
            model.addAttribute("choreForm", form);
            model.addAttribute("choreId", id);
            model.addAttribute("choreTemplates", CHORE_TEMPLATES);
            return "chore/edit";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/chores";
        }
    }

    /**
     * お手伝ーを更新する。
     * 成否を問わず {@code /chores} にリダイレクトする。
     */
    @PostMapping("/{id}/edit")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         @Valid @ModelAttribute ChoreForm choreForm,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("choreId", id);
            model.addAttribute("choreTemplates", CHORE_TEMPLATES);
            return "chore/edit";
        }
        try {
            choreService.updateChore(id, userDetails.getUsername(), choreForm);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/chores";
    }

    /**
     * お手伝いを削除する。
     * 成否を問わず {@code /chores} にリダイレクトする。
     */
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
