package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.AllowanceRequest;
import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import design.hitsuji.otetsudai.enums.RequestStatus;
import design.hitsuji.otetsudai.repository.AllowanceRequestRepository;
import design.hitsuji.otetsudai.repository.ChoreRepository;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * お小遣い申請のビジネスロジックを担うサービス。
 *
 * <p>金額計算ルール（同日基準）:
 * <ul>
 *   <li>1件: ¥20</li>
 *   <li>2件: ¥50</li>
 *   <li>3件以上: ¥50 × (n − 1)</li>
 * </ul>
 */
@Service
@Transactional
public class AllowanceService {

    /** 1件/日のお手伝いに対する支給額（円）。 */
    private static final int SINGLE_CHORE_AMOUNT = 20;
    /** 2件/日のお手伝いに対する支給額（円）。 */
    private static final int DOUBLE_CHORE_AMOUNT = 50;
    /** 3件/日以上のお手伝い1件追加ごとの加算額（円）。 */
    private static final int ADDITIONAL_CHORE_AMOUNT = 50;

    private final AllowanceRequestRepository allowanceRequestRepository;
    private final ChoreRepository choreRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AllowanceService(AllowanceRequestRepository allowanceRequestRepository,
                            ChoreRepository choreRepository,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.allowanceRequestRepository = allowanceRequestRepository;
        this.choreRepository = choreRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * お手伝いリストに基づいてお小遣い金額を計算する。
     *
     * <p>計算は日付ごとのお手伝い件数を基準とし、日をまたいだ件数は合算しない。
     *
     * @param chores 対象のお手伝いリスト
     * @return 合計金額（円）
     */
    public int calculateAmount(List<Chore> chores) {
        Map<LocalDate, Long> countByDate = chores.stream()
                .collect(Collectors.groupingBy(Chore::getChoreDate, Collectors.counting()));

        int total = 0;
        for (long n : countByDate.values()) {
            if (n == 1) {
                total += SINGLE_CHORE_AMOUNT;
            } else if (n == 2) {
                total += DOUBLE_CHORE_AMOUNT;
            } else {
                total += ADDITIONAL_CHORE_AMOUNT * (n - 1);
            }
        }
        return total;
    }

    /**
     * 申請前の金額プレビューを返す（AJAX用）。
     * 対象チェックは行わず、見つかった件数で計算する。
     *
     * @param loginUserId ログイン中の子どものユーザーID
     * @param choreIds    選択されたお手伝いIDのリスト
     * @return プレビュー金額（円）。リストが空の場合は 0。
     */
    @Transactional(readOnly = true)
    public int previewAmount(String loginUserId, List<Long> choreIds) {
        if (choreIds == null || choreIds.isEmpty()) {
            return 0;
        }
        User user = findUserByLoginId(loginUserId);
        List<Chore> chores = choreRepository.findByIdInAndUserId(choreIds, user.getId());
        return calculateAmount(chores);
    }

    /**
     * お小遣い申請を作成し、対象お手伝いのステータスを {@code PENDING} に変更する。
     *
     * @param loginUserId ログイン中の子どものユーザーID
     * @param choreIds    申請に含めるお手伝いIDのリスト
     * @return 保存されたお小遣い申請
     * @throws IllegalStateException リストが空・他ユーザーのお手伝いが含まれる・UNPAID以外が含まれる場合
     */
    public AllowanceRequest createRequest(String loginUserId, List<Long> choreIds) {
        if (choreIds == null || choreIds.isEmpty()) {
            throw new IllegalStateException("お手伝いを1件以上選択してください");
        }

        User user = findUserByLoginId(loginUserId);
        List<Chore> chores = choreRepository.findByIdInAndUserId(choreIds, user.getId());

        if (chores.size() != choreIds.size()) {
            throw new IllegalStateException("選択したお手伝いに無効なものが含まれています");
        }

        for (Chore chore : chores) {
            if (chore.getStatus() != ChoreStatus.UNPAID) {
                throw new IllegalStateException("申請済みのお手伝いが含まれています");
            }
        }

        int amount = calculateAmount(chores);

        AllowanceRequest request = new AllowanceRequest();
        request.setChildUserId(user.getId());
        request.setRequestDate(LocalDate.now());
        request.setTotalAmount(amount);
        AllowanceRequest savedRequest = allowanceRequestRepository.save(request);

        for (Chore chore : chores) {
            chore.setStatus(ChoreStatus.PENDING);
            chore.setRequestId(savedRequest.getId());
            choreRepository.save(chore);
        }

        notificationService.notifyParentAllowanceRequested(user, savedRequest, chores);
        return savedRequest;
    }

    /**
     * 承認待ちの申請一覧を返す（親向け）。
     *
     * @return 作成日時降順の承認待ち申請リスト
     */
    @Transactional(readOnly = true)
    public List<AllowanceRequest> listPendingRequests() {
        return allowanceRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING);
    }

    /**
     * 申請詳細を取得する。{@code PENDING} 以外の申請は例外とする。
     *
     * @param requestId 申請ID
     * @throws IllegalStateException 申請が存在しない、または既に処理済みの場合
     */
    @Transactional(readOnly = true)
    public AllowanceRequest getRequest(Long requestId) {
        AllowanceRequest request = allowanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("申請が見つかりません"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("この申請は既に処理されています");
        }
        return request;
    }

    /** 申請に紐づくお手伝い一覧を返す（詳細画面表示用）。 */
    @Transactional(readOnly = true)
    public List<Chore> getChoresForRequest(Long requestId) {
        return choreRepository.findByRequestId(requestId);
    }

    /**
     * 申請を承認し、対象お手伝いのステータスを {@code APPROVED} に変更する。
     *
     * @param requestId 申請ID
     * @throws IllegalStateException 申請が存在しない、または {@code PENDING} 以外の場合
     */
    public void approveRequest(Long requestId) {
        AllowanceRequest request = allowanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("申請が見つかりません"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("承認できない状態の申請です");
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedAt(LocalDateTime.now());
        allowanceRequestRepository.save(request);

        List<Chore> chores = choreRepository.findByRequestId(requestId);
        chores.forEach(chore -> chore.setStatus(ChoreStatus.APPROVED));
        choreRepository.saveAll(chores);
    }

    /**
     * 申請を却下し、対象お手伝いのステータスを {@code UNPAID} に差し戻す。
     *
     * @param requestId 申請ID
     * @param reason    却下理由（任意）
     * @throws IllegalStateException 申請が存在しない、または {@code PENDING} 以外の場合
     */
    public void rejectRequest(Long requestId, String reason) {
        AllowanceRequest request = allowanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("申請が見つかりません"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("却下できない状態の申請です");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);
        allowanceRequestRepository.save(request);

        List<Chore> chores = choreRepository.findByRequestId(requestId);
        chores.forEach(chore -> {
            chore.setStatus(ChoreStatus.UNPAID);
            chore.setRequestId(null);
        });
        choreRepository.saveAll(chores);
    }

    /**
     * 承認済みの申請履歴を返す（親向け履歴画面）。
     *
     * @return 承認日時降順の申請リスト
     */
    @Transactional(readOnly = true)
    public List<AllowanceRequest> listApprovedHistory() {
        return allowanceRequestRepository.findByStatusOrderByApprovedAtDesc(RequestStatus.APPROVED);
    }

    /**
     * 子どものユーザーIDからログインIDを返す（一覧表示のラベル用）。
     * ユーザーが見つからない場合は {@code "不明"} を返す。
     */
    @Transactional(readOnly = true)
    public String getChildLoginId(Long childUserId) {
        return userRepository.findById(childUserId)
                .map(User::getUserId)
                .orElse("不明");
    }

    private User findUserByLoginId(String loginUserId) {
        return userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));
    }
}
