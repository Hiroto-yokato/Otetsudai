package design.hitsuji.otetsudai.controller;

import design.hitsuji.otetsudai.dto.UserRegistrationForm;
import design.hitsuji.otetsudai.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * 親による子どもアカウント管理を担うコントローラー（{@code /users}）。
 *
 * <p>SecurityConfig で {@code /users/**} は {@code ROLE_PARENT} のみアクセス可能。
 */
@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 家族の子どもアカウント一覧を表示する。 */
    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("children", userService.listChildren(userDetails.getUsername()));
        return "users/list";
    }

    /** 子どもアカウント登録フォームを表示する。 */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserRegistrationForm());
        return "users/new";
    }

    /**
     * 子どもアカウントを登録する。
     * 成功時は {@code /users?registered=true} にリダイレクトする。
     */
    @PostMapping
    public String register(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("form") UserRegistrationForm form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "users/new";
        }

        try {
            userService.registerChild(userDetails.getUsername(), form);
            return "redirect:/users?registered=true";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "users/new";
        }
    }
}
