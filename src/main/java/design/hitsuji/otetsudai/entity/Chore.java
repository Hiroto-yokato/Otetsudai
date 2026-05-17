package design.hitsuji.otetsudai.entity;

import design.hitsuji.otetsudai.enums.ChoreStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chores")
public class Chore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "chore_date", nullable = false)
    private LocalDate choreDate;

    @Column(nullable = false, length = 100)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChoreStatus status;

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
