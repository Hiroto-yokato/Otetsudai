package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.dto.ParentRegistrationForm;
import design.hitsuji.otetsudai.dto.UserRegistrationForm;
import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.Role;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerChild(String loginUserId, UserRegistrationForm form) {
        User parent = findUserByLoginId(loginUserId);

        if (userRepository.existsByUserId(form.getUserId())) {
            throw new IllegalStateException("このユーザーIDは既に使用されています");
        }

        if (!form.getPassword().equals(form.getPasswordConfirm())) {
            throw new IllegalStateException("パスワードが一致しません");
        }

        String email = StringUtils.hasText(form.getEmail()) ? form.getEmail() : null;
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalStateException("このメールアドレスは既に使用されています");
        }

        User child = new User();
        child.setUserId(form.getUserId());
        child.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        child.setEmail(email);
        child.setDisplayName(StringUtils.hasText(form.getDisplayName()) ? form.getDisplayName() : null);
        child.setRole(Role.CHILD);
        child.setFamilyId(parent.getFamilyId());

        return userRepository.save(child);
    }

    public User registerParent(ParentRegistrationForm form) {
        if (userRepository.existsByUserId(form.getUserId())) {
            throw new IllegalStateException("このユーザーIDは既に使用されています");
        }

        if (!form.getPassword().equals(form.getPasswordConfirm())) {
            throw new IllegalStateException("パスワードが一致しません");
        }

        if (userRepository.existsByEmail(form.getEmail())) {
            throw new IllegalStateException("このメールアドレスは既に使用されています");
        }

        User parent = new User();
        parent.setUserId(form.getUserId());
        parent.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        parent.setEmail(form.getEmail());
        parent.setDisplayName(StringUtils.hasText(form.getDisplayName()) ? form.getDisplayName() : null);
        parent.setRole(Role.PARENT);

        User saved = userRepository.save(parent);
        saved.setFamilyId(saved.getId());
        return userRepository.save(saved);
    }

    @Transactional(readOnly = true)
    public List<User> listChildren(String loginUserId) {
        User parent = findUserByLoginId(loginUserId);
        if (parent.getFamilyId() == null) {
            return List.of();
        }
        return userRepository.findByRoleAndFamilyId(Role.CHILD, parent.getFamilyId());
    }

    private User findUserByLoginId(String loginUserId) {
        return userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));
    }
}
