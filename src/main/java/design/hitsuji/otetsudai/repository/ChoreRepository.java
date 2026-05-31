package design.hitsuji.otetsudai.repository;

import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@link Chore} エンティティのリポジトリ。
 * メソッド名からクエリを自動生成する Spring Data JPA の派生クエリを使用する。
 */
public interface ChoreRepository extends JpaRepository<Chore, Long> {

    /** 指定ユーザーのお手伝いを日付降順で取得する。 */
    List<Chore> findByUserIdOrderByChoreDateDesc(Long userId);

    /** 複数ユーザーのお手伝いをまとめて日付降順で取得する（家族一覧表示用）。 */
    List<Chore> findByUserIdInOrderByChoreDateDesc(List<Long> userIds);

    /** 指定ユーザーの指定ステータスのお手伝いを取得する。 */
    List<Chore> findByUserIdAndStatus(Long userId, ChoreStatus status);

    /**
     * 指定IDリストのうち、かつ指定ユーザーに属するお手伝いを取得する。
     * 返却件数と指定ID数が一致するか確認することで、他ユーザーのデータへの不正アクセスを検出する。
     */
    List<Chore> findByIdInAndUserId(List<Long> ids, Long userId);

    /** 指定申請IDに紐づくお手伝いを取得する（承認・却下時の状態更新用）。 */
    List<Chore> findByRequestId(Long requestId);
}
