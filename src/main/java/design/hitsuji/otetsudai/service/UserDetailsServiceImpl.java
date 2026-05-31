package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security に登録するユーザー詳細サービスの実装。
 *
 * <p>ログインIDはユーザーIDのみを受け付ける。
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * ユーザーIDでユーザーを検索し、Spring Security の {@link UserDetails} を返す。
     *
     * @param loginId ログインフォームに入力されたユーザーID
     * @throws UsernameNotFoundException ユーザーが見つからない場合
     */
    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません"));

        String roleAuthority = "ROLE_" + user.getRole().name();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(roleAuthority)))
                .build();
    }
}
