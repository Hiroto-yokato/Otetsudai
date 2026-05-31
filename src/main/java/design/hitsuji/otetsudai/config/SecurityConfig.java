package design.hitsuji.otetsudai.config;

import design.hitsuji.otetsudai.service.AuthSuccessHandler;
import design.hitsuji.otetsudai.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security の設定クラス。
 *
 * <p>URLアクセス制御は先勝ち（first-match-wins）のため、より限定的なルールを先に定義する。
 * {@code /chores/family}（PARENT限定）は {@code /chores/**}（CHILD限定）より前に宣言している。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthSuccessHandler authSuccessHandler;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          AuthSuccessHandler authSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.authSuccessHandler = authSuccessHandler;
    }

    /**
     * セキュリティフィルターチェーンを構成する。
     *
     * <ul>
     *   <li>ログイン成功後のリダイレクト先はロールに応じて {@link AuthSuccessHandler} が決定する。</li>
     *   <li>パスワードは BCrypt strength=12 でハッシュ化する。</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
                .requestMatchers("/approvals/**", "/history", "/users/**", "/chores/family").hasRole("PARENT")
                .requestMatchers("/chores/**", "/requests/**").hasRole("CHILD")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(authSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .userDetailsService(userDetailsService);

        return http.build();
    }

    /** BCrypt strength=12 のパスワードエンコーダー。 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
