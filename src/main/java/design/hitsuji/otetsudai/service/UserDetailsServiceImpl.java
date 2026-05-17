package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByUserIdOrEmail(loginId, loginId)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません"));

        String roleAuthority = "ROLE_" + user.getRole().name();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(roleAuthority)))
                .build();
    }
}
