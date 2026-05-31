package design.hitsuji.otetsudai.controller;

import design.hitsuji.otetsudai.dto.ParentRegistrationForm;
import design.hitsuji.otetsudai.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * ログイン・親アカウント自己登録を担うコントローラー。
 *
 * <p>ログイン処理自体は Spring Security が行う。
 * このコントローラーはログイン画面の表示と親の自己登録のみを担う。
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** ログイン画面を表示する。 */
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    /** 親アカウント登録画面を表示する。 */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new ParentRegistrationForm());
        return "auth/register";
    }

    /**
     * 親アカウントを登録する。
     * 成功時は {@code /login?registered=true} にリダイレクトする。
     */
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") ParentRegistrationForm form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerParent(form);
            return "redirect:/login?registered=true";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }
}
