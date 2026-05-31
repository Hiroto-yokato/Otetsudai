package design.hitsuji.otetsudai.entity;

import design.hitsuji.otetsudai.enums.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ユーザーを表すエンティティ（テーブル名: {@code users}）。
 *
 * <p>{@code familyId} によって親と子どもが同一家族として紐づく。
 * 親ユーザーは自身の {@code id} を {@code familyId} に設定し、
 * 子どもは登録時に親の {@code familyId} を引き継ぐ。
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ログインに使用する一意のユーザーID（英数字・アンダースコア）。 */
    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    /** メールアドレス。親は必須、子どもは任意。通知メールの宛先として使用。 */
    @Column(unique = true)
    private String email;

    /** BCryptでハッシュ化されたパスワード。 */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * 家族を識別するID。同一家族の親・子どもが同じ値を持つ。
     * 親の場合は {@code users.id} と同値。
     */
    @Column(name = "family_id")
    private Long familyId;

    /** 画面上の表示名。未設定の場合は {@code userId} を代わりに使用する。 */
    @Column(name = "display_name", length = 50)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Long getFamilyId() { return familyId; }
    public void setFamilyId(Long familyId) { this.familyId = familyId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
