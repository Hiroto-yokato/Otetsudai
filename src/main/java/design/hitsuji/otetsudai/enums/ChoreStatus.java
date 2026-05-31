package design.hitsuji.otetsudai.enums;

/**
 * お手伝いの状態を表す列挙型。
 *
 * <p>状態遷移: {@code UNPAID → PENDING → APPROVED / REJECTED}
 * <ul>
 *   <li>{@code UNPAID}   — 登録済み・未申請。編集・削除が可能。</li>
 *   <li>{@code PENDING}  — お小遣い申請に含まれ、親の承認待ち。</li>
 *   <li>{@code APPROVED} — 親が承認済み。</li>
 *   <li>{@code REJECTED} — 親が却下。{@code UNPAID} に差し戻される。</li>
 * </ul>
 */
public enum ChoreStatus {
    UNPAID, PENDING, APPROVED, REJECTED
}
