package design.hitsuji.otetsudai.service;

import design.hitsuji.otetsudai.entity.AllowanceRequest;
import design.hitsuji.otetsudai.entity.Chore;
import design.hitsuji.otetsudai.entity.User;
import design.hitsuji.otetsudai.enums.Role;
import design.hitsuji.otetsudai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 家族内のイベント（お手伝い登録・お小遣い申請）を親アカウントへメールで通知するサービス。
 *
 * <p>メール送信が失敗しても呼び出し元のビジネスロジックに影響しないよう、
 * 全メソッド内で例外を catch してログに記録するのみとする。
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M月d日");

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    public NotificationService(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    /**
     * 子どもが単体でお手伝いを登録したことを親に通知する。
     *
     * @param child 登録を行った子どものユーザー
     * @param chore 登録されたお手伝い
     */
    public void notifyParentChoreRegistered(User child, Chore chore) {
        User parent = findParent(child);
        if (parent == null || parent.getEmail() == null) return;

        String childName = displayName(child);
        String subject = childName + "がお手伝いを登録しました";
        String body = childName + "が以下のお手伝いを登録しました。\n\n"
                + "日付: " + chore.getChoreDate().format(DATE_FMT) + "\n"
                + "内容: " + chore.getContent() + "\n\n"
                + "アプリでお小遣い申請をお待ちください。";

        send(parent.getEmail(), subject, body);
    }

    /**
     * 子どもが複数のお手伝いを一括登録したことを親に通知する。
     *
     * @param child  登録を行った子どものユーザー
     * @param chores 登録されたお手伝いのリスト（空でないことが前提）
     */
    public void notifyParentBulkChoreRegistered(User child, List<Chore> chores) {
        User parent = findParent(child);
        if (parent == null || parent.getEmail() == null) return;

        String childName = displayName(child);
        String subject = childName + "がお手伝いを" + chores.size() + "件まとめて登録しました";
        String choreList = chores.stream()
                .map(c -> "・" + c.getChoreDate().format(DATE_FMT) + " " + c.getContent())
                .collect(Collectors.joining("\n"));
        String body = childName + "が以下のお手伝いを登録しました。\n\n"
                + choreList + "\n\n"
                + "アプリでお小遣い申請をお待ちください。";

        send(parent.getEmail(), subject, body);
    }

    /**
     * 子どもがお小遣いを申請したことを親に通知する。
     *
     * @param child   申請を行った子どものユーザー
     * @param request 作成されたお小遣い申請
     * @param chores  申請に含まれるお手伝いのリスト
     */
    public void notifyParentAllowanceRequested(User child, AllowanceRequest request, List<Chore> chores) {
        User parent = findParent(child);
        if (parent == null || parent.getEmail() == null) return;

        String childName = displayName(child);
        String subject = childName + "がお小遣いを申請しました（¥" + request.getTotalAmount() + "）";
        String body = childName + "がお小遣いを申請しました。\n\n"
                + "申請金額: ¥" + request.getTotalAmount() + "\n"
                + "対象お手伝い: " + chores.size() + "件\n\n"
                + "アプリにログインして承認をお願いします。";

        send(parent.getEmail(), subject, body);
    }

    /**
     * 子どもと同じ familyId を持つ最初の親ユーザーを返す。
     * familyId が未設定、または親が存在しない場合は {@code null} を返す。
     */
    private User findParent(User child) {
        if (child.getFamilyId() == null) return null;
        List<User> parents = userRepository.findByRoleAndFamilyId(Role.PARENT, child.getFamilyId());
        return parents.isEmpty() ? null : parents.get(0);
    }

    /** displayName が設定されていない場合は userId を代わりに使用する。 */
    private String displayName(User user) {
        return user.getDisplayName() != null ? user.getDisplayName() : user.getUserId();
    }

    /**
     * テキストメールを送信する。送信失敗は例外を伝播させず {@code log.warn} のみで記録する。
     * これにより SMTP 障害がビジネス処理に影響しない。
     */
    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("メール送信失敗 to={}: {}", to, e.getMessage());
        }
    }
}
