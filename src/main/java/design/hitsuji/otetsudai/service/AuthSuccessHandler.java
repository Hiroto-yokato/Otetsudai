package design.hitsuji.otetsudai.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ログイン成功後のリダイレクト先をロールに応じて振り分けるハンドラー。
 *
 * <ul>
 *   <li>PARENT → {@code /approvals}（申請一覧）</li>
 *   <li>CHILD  → {@code /chores}（お手伝い一覧）</li>
 *   <li>その他 → {@code /login?error}</li>
 * </ul>
 */
@Component
public class AuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String contextPath = request.getContextPath();
        boolean isParent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"));
        boolean isChild = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CHILD"));

        if (isParent) {
            response.sendRedirect(contextPath + "/approvals");
        } else if (isChild) {
            response.sendRedirect(contextPath + "/chores");
        } else {
            response.sendRedirect(contextPath + "/login?error");
        }
    }
}
