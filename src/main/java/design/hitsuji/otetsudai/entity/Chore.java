package design.hitsuji.otetsudai.entity;

import design.hitsuji.otetsudai.enums.ChoreStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * お手伝いの記録を表すエンティティ（テーブル名: {@code chores}）。
 *
 * <p>{@code userId} は {@link User#getId()} への外部キーだが、
 * JPA の {@code @ManyToOne} ではなく Long で保持する（JOIN不要のため）。
 * {@code requestId} はお小遣い申請に紐づいた際に設定される。
 */
@Entity
@Table(name = "chores")
public class Chore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** このお手伝いを登録した子どものユーザーID（{@code users.id} への参照）。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "chore_date", nullable = false)
    private LocalDate choreDate;

    /** お手伝いの内容。最大100文字。 */
    @Column(nullable = false, length = 100)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChoreStatus status;

    /**
     * 紐づくお小遣い申請のID（{@code allowance_requests.id} への参照）。
     * 申請前は {@code null}、申請後に設定される。
     */
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ChoreStatus.UNPAID;
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getChoreDate() { return choreDate; }
    public void setChoreDate(LocalDate choreDate) { this.choreDate = choreDate; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public ChoreStatus getStatus() { return status; }
    public void setStatus(ChoreStatus status) { this.status = status; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
