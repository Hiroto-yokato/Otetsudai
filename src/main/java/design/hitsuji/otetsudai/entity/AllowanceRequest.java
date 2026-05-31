package design.hitsuji.otetsudai.entity;

import design.hitsuji.otetsudai.enums.RequestStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * お小遣い申請を表すエンティティ（テーブル名: {@code allowance_requests}）。
 *
 * <p>1件の申請は複数のお手伝い（{@link Chore}）を束ねる。
 * {@code totalAmount} は申請時点の計算結果を永続化した値で、
 * 承認後にお手伝いが変更されても影響を受けない。
 */
@Entity
@Table(name = "allowance_requests")
public class AllowanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 申請を行った子どものユーザーID（{@code users.id} への参照）。 */
    @Column(name = "child_user_id", nullable = false)
    private Long childUserId;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    /** 申請金額（円）。お手伝い件数・日付に基づく計算結果を保存する。 */
    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    /** 却下理由。{@code status = REJECTED} のときのみ設定される。 */
    @Column(name = "rejection_reason")
    private String rejectionReason;

    /** 承認日時。{@code status = APPROVED} のときのみ設定される。 */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

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
            this.status = RequestStatus.PENDING;
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Long getChildUserId() { return childUserId; }
    public void setChildUserId(Long childUserId) { this.childUserId = childUserId; }

    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
