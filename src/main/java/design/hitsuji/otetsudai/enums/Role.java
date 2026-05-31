package design.hitsuji.otetsudai.enums;

/**
 * ユーザーのロールを表す列挙型。
 * Spring Security の権限名は {@code ROLE_PARENT} / {@code ROLE_CHILD} となる。
 */
public enum Role {
    /** 親（承認・家族管理を行う側） */
    PARENT,
    /** 子ども（お手伝い登録・お小遣い申請を行う側） */
    CHILD
}
