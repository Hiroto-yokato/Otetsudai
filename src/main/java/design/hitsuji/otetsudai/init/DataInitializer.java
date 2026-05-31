package design.hitsuji.otetsudai.init;

import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.Role;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 開発・デフォルト環境のみ実行される初期データ投入クラス。
 *
 * <p>DBにユーザーが1件も存在しない場合に限り、以下のデモ用アカウントを作成する:
 * <ul>
 *   <li>親: userId={@code parent} / password={@code parent123}</li>
 *   <li>子ども: userId={@code child} / password={@code child123}</li>
 * </ul>
 * {@code prod} プロファイルでは実行されない。
 */
@Component
@Profile({"dev", "default"})
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User parent = new User();
        parent.setUserId("parent");
        parent.setEmail("parent@example.com");
        parent.setPasswordHash(passwordEncoder.encode("parent123"));
        parent.setRole(Role.PARENT);
        parent.setFamilyId(1L);
        userRepository.save(parent);

        User child = new User();
        child.setUserId("child");
        child.setPasswordHash(passwordEncoder.encode("child123"));
        child.setRole(Role.CHILD);
        child.setFamilyId(1L);
        userRepository.save(child);
    }
}
