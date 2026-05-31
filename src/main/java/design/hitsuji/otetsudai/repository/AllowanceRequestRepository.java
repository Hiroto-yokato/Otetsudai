package design.hitsuji.otetsudai.repository;

import design.hitsuji.otetsudai.entity.AllowanceRequest;
import design.hitsuji.otetsudai.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * {@link AllowanceRequest} エンティティのリポジトリ。
 */
public interface AllowanceRequestRepository extends JpaRepository<AllowanceRequest, Long> {

    /** 指定ステータスの申請を作成日時降順で取得する（承認待ち一覧用）。 */
    List<AllowanceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    /** 指定ステータスの申請を承認日時降順で取得する（承認済み履歴用）。 */
    List<AllowanceRequest> findByStatusOrderByApprovedAtDesc(RequestStatus status);

    /** 指定ユーザーの指定ステータスの申請を取得する。 */
    List<AllowanceRequest> findByChildUserIdAndStatus(Long childUserId, RequestStatus status);
}
