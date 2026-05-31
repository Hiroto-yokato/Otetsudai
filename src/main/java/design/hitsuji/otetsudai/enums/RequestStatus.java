package design.hitsuji.otetsudai.enums;

/**
 * お小遣い申請の状態を表す列挙型。
 *
 * <p>状態遷移: {@code PENDING → APPROVED / REJECTED}
 * <ul>
 *   <li>{@code PENDING}  — 申請済み・親の承認待ち。</li>
 *   <li>{@code APPROVED} — 親が承認済み。</li>
 *   <li>{@code REJECTED} — 親が却下。対象お手伝いは {@code UNPAID} に差し戻される。</li>
 * </ul>
 */
public enum RequestStatus {
    PENDING, APPROVED, REJECTED
}
