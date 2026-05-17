package design.hitsuji.otetsudai.repository;

import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    Optional<User> findByEmail(String email);

    Optional<User> findByUserIdOrEmail(String userId, String email);

    List<User> findByRoleAndFamilyId(Role role, Long familyId);

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);
}
