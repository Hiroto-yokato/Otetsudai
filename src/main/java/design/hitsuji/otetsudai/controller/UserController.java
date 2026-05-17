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

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("children", userService.listChildren(userDetails.getUsername()));
        return "users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserRegistrationForm());
        return "users/new";
    }

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
