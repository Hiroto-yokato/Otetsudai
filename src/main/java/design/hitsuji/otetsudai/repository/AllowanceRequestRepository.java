package design.hitsuji.otetsudai.repository;

import design.hitsuji.otetsudai.entity.AllowanceRequest;
import design.hitsuji.otetsudai.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AllowanceRequestRepository extends JpaRepository<AllowanceRequest, Long> {
    List<AllowanceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    List<AllowanceRequest> findByStatusOrderByApprovedAtDesc(RequestStatus status);
    List<AllowanceRequest> findByChildUserIdAndStatus(Long childUserId, RequestStatus status);
}
