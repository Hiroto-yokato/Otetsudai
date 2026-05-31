package design.hitsuji.otetsudai.repository;

import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@link User} エンティティのリポジトリ。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** ユーザーIDで検索する。 */
    Optional<User> findByUserId(String userId);

    /** メールアドレスで検索する。 */
    Optional<User> findByEmail(String email);

    /**
     * ユーザーIDまたはメールアドレスのどちらかが一致するユーザーを検索する。
     * ログイン時に両方の値に同じ入力値を渡すことで、ID・メール両方でのログインを実現する。
     */
    Optional<User> findByUserIdOrEmail(String userId, String email);

    /** 指定ロール・家族IDのユーザー一覧を取得する（子ども一覧・親検索に使用）。 */
    List<User> findByRoleAndFamilyId(Role role, Long familyId);

    /** ユーザーIDの重複チェック。 */
    boolean existsByUserId(String userId);

    /** メールアドレスの重複チェック。 */
    boolean existsByEmail(String email);
}
