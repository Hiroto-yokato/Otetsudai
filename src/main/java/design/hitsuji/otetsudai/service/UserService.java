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

/**
 * ユーザー管理のビジネスロジックを担うサービス。
 *
 * <p>親アカウントの自己登録と、親による子どもアカウント登録の2系統を扱う。
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 子どもアカウントを登録する。
     *
     * <p>子どもは操作を行った親の {@code familyId} を引き継ぐ。
     *
     * @param loginUserId 操作中の親のユーザーID
     * @param form        子ども登録フォーム
     * @throws IllegalStateException ユーザーID・メールアドレスの重複、またはパスワード不一致の場合
     */
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

    /**
     * 親アカウントを自己登録する。
     *
     * <p>親の {@code familyId} は自身の {@code id}（DB採番後）と同値に設定する。
     * そのため save を2回呼び出す。
     *
     * @param form 親登録フォーム（メールアドレス必須）
     * @throws IllegalStateException ユーザーID・メールアドレスの重複、またはパスワード不一致の場合
     */
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

    /**
     * 指定した親に属する子どもの一覧を返す。
     *
     * @param loginUserId 操作中の親のユーザーID
     * @return 子どものリスト。familyId 未設定の場合は空リスト。
     */
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
