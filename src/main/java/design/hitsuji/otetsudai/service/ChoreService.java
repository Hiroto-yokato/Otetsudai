package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.dto.BulkChoreForm;
import design.hitsuji.otetsudai.dto.ChoreForm;
import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.ChoreStatus;
import design.hitsuji.otetsudai.enums.Role;
import design.hitsuji.otetsudai.repository.ChoreRepository;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * お手伝い登録・編集・削除のビジネスロジックを担うサービス。
 *
 * <p>編集・削除の権限チェック（所有者確認・ステータス確認）は
 * {@link #findEditableChore} に集約している。
 */
@Service
@Transactional
public class ChoreService {

    private final ChoreRepository choreRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ChoreService(ChoreRepository choreRepository, UserRepository userRepository,
                        NotificationService notificationService) {
        this.choreRepository = choreRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * ログインユーザーのお手伝い一覧を日付降順で返す。
     *
     * @param loginUserId ログイン中の子どものユーザーID
     */
    @Transactional(readOnly = true)
    public List<Chore> listChores(String loginUserId) {
        User user = findUserByLoginId(loginUserId);
        return choreRepository.findByUserIdOrderByChoreDateDesc(user.getId());
    }

    /**
     * ログインユーザーの未申請（{@code UNPAID}）お手伝いを返す。
     * お小遣い申請の対象候補として使用する。
     *
     * @param loginUserId ログイン中の子どものユーザーID
     */
    @Transactional(readOnly = true)
    public List<Chore> listUnpaidChores(String loginUserId) {
        User user = findUserByLoginId(loginUserId);
        return choreRepository.findByUserIdAndStatus(user.getId(), ChoreStatus.UNPAID);
    }

    /**
     * 複数のお手伝いを一括登録する。
     *
     * <p>content が空白のみの行はスキップする。全行空白の場合は例外をスローする。
     * 未来日付は登録不可。content は前後の空白をトリムしてから保存する。
     *
     * @param loginUserId ログイン中の子どものユーザーID
     * @param form        一括登録フォーム
     * @return 保存されたお手伝いのリスト（空行を除く）
     * @throws IllegalStateException 全行空白・未来日付・100文字超の場合
     */
    public List<Chore> createChoresBulk(String loginUserId, BulkChoreForm form) {
        User user = findUserByLoginId(loginUserId);
        LocalDate today = LocalDate.now();
        List<Chore> saved = new ArrayList<>();
        for (BulkChoreForm.ChoreEntryForm entry : form.getEntries()) {
            if (!org.springframework.util.StringUtils.hasText(entry.getContent())) {
                continue;
            }
            String content = entry.getContent().trim();
            if (content.length() > 100) {
                throw new IllegalStateException("お手伝いの内容は100文字以内で入力してください");
            }
            LocalDate date = entry.getChoreDate() != null ? entry.getChoreDate() : today;
            if (date.isAfter(today)) {
                throw new IllegalStateException("未来の日付は登録できません");
            }
            Chore chore = new Chore();
            chore.setUserId(user.getId());
            chore.setChoreDate(date);
            chore.setContent(content);
            chore.setStatus(ChoreStatus.UNPAID);
            saved.add(choreRepository.save(chore));
        }
        if (saved.isEmpty()) {
            throw new IllegalStateException("お手伝いが1件も入力されていません");
        }
        notificationService.notifyParentBulkChoreRegistered(user, saved);
        return saved;
    }

    /**
     * お手伝いを1件登録する。
     *
     * @param loginUserId ログイン中の子どものユーザーID
     * @param choreDate   お手伝いの日付
     * @param content     お手伝いの内容
     * @return 保存されたお手伝い
     */
    public Chore createChore(String loginUserId, LocalDate choreDate, String content) {
        User user = findUserByLoginId(loginUserId);
        Chore chore = new Chore();
        chore.setUserId(user.getId());
        chore.setChoreDate(choreDate);
        chore.setContent(content);
        chore.setStatus(ChoreStatus.UNPAID);
        Chore saved = choreRepository.save(chore);
        notificationService.notifyParentChoreRegistered(user, saved);
        return saved;
    }

    /**
     * 編集対象のお手伝いを取得する（権限チェック込み）。
     *
     * @param choreId     対象のお手伝いID
     * @param loginUserId ログイン中の子どものユーザーID
     * @throws IllegalStateException  お手伝いが見つからない・{@code UNPAID} 以外の場合
     * @throws AccessDeniedException  他ユーザーのお手伝いの場合
     */
    @Transactional(readOnly = true)
    public Chore getChoreForEdit(Long choreId, String loginUserId) {
        User user = findUserByLoginId(loginUserId);
        return findEditableChore(choreId, user.getId());
    }

    /**
     * お手伝いの内容・日付を更新する。
     *
     * @param choreId     対象のお手伝いID
     * @param loginUserId ログイン中の子どものユーザーID
     * @param form        更新内容
     * @return 更新後のお手伝い
     * @throws IllegalStateException  お手伝いが見つからない・{@code UNPAID} 以外の場合
     * @throws AccessDeniedException  他ユーザーのお手伝いの場合
     */
    public Chore updateChore(Long choreId, String loginUserId, ChoreForm form) {
        User user = findUserByLoginId(loginUserId);
        Chore chore = findEditableChore(choreId, user.getId());
        chore.setChoreDate(form.getChoreDate());
        chore.setContent(form.getContent());
        return choreRepository.save(chore);
    }

    /**
     * 同一家族の全子どものお手伝い一覧を返す（親専用）。
     *
     * @param loginUserId ログイン中の親のユーザーID
     * @throws AccessDeniedException PARENT 以外が呼び出した場合
     */
    @Transactional(readOnly = true)
    public List<Chore> listFamilyChores(String loginUserId) {
        User user = findUserByLoginId(loginUserId);
        if (user.getRole() != Role.PARENT) {
            throw new AccessDeniedException("親アカウントのみ閲覧できます");
        }
        if (user.getFamilyId() == null) {
            return List.of();
        }
        List<User> children = userRepository.findByRoleAndFamilyId(Role.CHILD, user.getFamilyId());
        if (children.isEmpty()) {
            return List.of();
        }
        List<Long> childUserIds = children.stream().map(User::getId).toList();
        return choreRepository.findByUserIdInOrderByChoreDateDesc(childUserIds);
    }

    /**
     * ユーザーIDのリストから {@code userId → User} のマップを構築する。
     * 家族一覧画面でお手伝ーに紐づく子どもの表示名を引くために使用する。
     *
     * @param userIds ユーザーIDのリスト
     * @return ユーザーIDをキーとするマップ
     */
    @Transactional(readOnly = true)
    public Map<Long, User> buildUserMap(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    /**
     * お手伝いを削除する。
     *
     * @param choreId     対象のお手伝いID
     * @param loginUserId ログイン中の子どものユーザーID
     * @throws IllegalStateException  お手伝いが見つからない・{@code UNPAID} 以外の場合
     * @throws AccessDeniedException  他ユーザーのお手伝いの場合
     */
    public void deleteChore(Long choreId, String loginUserId) {
        User user = findUserByLoginId(loginUserId);
        Chore chore = findEditableChore(choreId, user.getId());
        choreRepository.delete(chore);
    }

    /**
     * 操作可能なお手伝いを取得する共通ヘルパー。
     * 所有者確認・ステータス確認（{@code UNPAID} のみ操作可）を行う。
     */
    private Chore findEditableChore(Long choreId, Long userId) {
        Chore chore = choreRepository.findById(choreId)
                .orElseThrow(() -> new IllegalStateException("お手伝いが見つかりません"));
        if (!chore.getUserId().equals(userId)) {
            throw new AccessDeniedException("このお手伝いを操作する権限がありません");
        }
        if (chore.getStatus() != ChoreStatus.UNPAID) {
            throw new IllegalStateException("申請中または承認済みのお手伝いは操作できません");
        }
        return chore;
    }

    private User findUserByLoginId(String loginUserId) {
        return userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"));
    }
}
