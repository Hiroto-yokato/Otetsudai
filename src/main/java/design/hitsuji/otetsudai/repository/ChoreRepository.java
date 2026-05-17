package design.hitsuji.otetsudai.repository;

import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChoreRepository extends JpaRepository<Chore, Long> {

    List<Chore> findByUserIdOrderByChoreDateDesc(Long userId);

    List<Chore> findByUserIdAndStatus(Long userId, ChoreStatus status);

    List<Chore> findByIdInAndUserId(List<Long> ids, Long userId);

    List<Chore> findByRequestId(Long requestId);
}
