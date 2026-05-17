# 設計書

## アーキテクチャ概要

お手伝い登録・お小遣い申請機能と同じレイヤードアーキテクチャ。既存の AllowanceService に承認系メソッドを追加し、新規 ApprovalController を作成する。

```
Browser
  ↓ GET /approvals
ApprovalController  ← PARENT ロールのみアクセス可
  ↓ listPendingRequests()
AllowanceService    → AllowanceRequestRepository

Browser
  ↓ GET /approvals/{id}
ApprovalController  ← PARENT ロールのみアクセス可
  ↓ getRequestDetail(requestId)
AllowanceService    → AllowanceRequestRepository + ChoreRepository（findByRequestId）

Browser
  ↓ POST /approvals/{id}/approve
ApprovalController → AllowanceService.approveRequest()
  ↓                  ├─ AllowanceRequest.status PENDING→APPROVED
                     ├─ AllowanceRequest.approvedAt = now
                     └─ Chore.status PENDING→APPROVED

Browser
  ↓ POST /approvals/{id}/reject
ApprovalController → AllowanceService.rejectRequest(reason)
  ↓                  ├─ AllowanceRequest.status PENDING→REJECTED
                     ├─ AllowanceRequest.rejectionReason = reason
                     └─ Chore.status PENDING→UNPAID, Chore.requestId = null

Browser
  ↓ GET /history
ApprovalController → AllowanceService.listApprovedHistory()
```

## コンポーネント設計

### 1. ChoreRepository.java 追記

```java
List<Chore> findByRequestId(Long requestId);
```

申請詳細ページとステータス更新に使用。

### 2. AllowanceService.java 追記

**listPendingRequests()**:
- `allowanceRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING)` を返す

**getRequestDetail(Long requestId)**:
- `allowanceRequestRepository.findById(requestId).orElseThrow()`
- `choreRepository.findByRequestId(requestId)`
- Map や DTO ではなく、Controller 側でモデルに分けてセットする

**approveRequest(Long requestId)**:
1. `allowanceRequestRepository.findById(requestId)` で取得
2. status が PENDING か確認 → 違う場合は `IllegalStateException`
3. `request.setStatus(APPROVED)`, `request.setApprovedAt(LocalDateTime.now())`
4. `allowanceRequestRepository.save(request)`
5. `choreRepository.findByRequestId(requestId)` で Chore 一覧取得
6. 各 Chore の status を APPROVED に変更して保存

**rejectRequest(Long requestId, String reason)**:
1. `allowanceRequestRepository.findById(requestId)` で取得
2. status が PENDING か確認 → 違う場合は `IllegalStateException`
3. `request.setStatus(REJECTED)`, `request.setRejectionReason(reason)`
4. `allowanceRequestRepository.save(request)`
5. `choreRepository.findByRequestId(requestId)` で Chore 一覧取得
6. 各 Chore の status を UNPAID、requestId を null に変更して保存

**listApprovedHistory()**:
- `allowanceRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.APPROVED)` を返す

### 3. ApprovalController.java（新規）

```java
@Controller
public class ApprovalController {

    GET  /approvals
        → model: pendingRequests
        → "approvals/list"

    GET  /approvals/{id}
        → model: request, chores
        → "approvals/detail"

    POST /approvals/{id}/approve
        → approveRequest(id) → redirect:/approvals
        → IllegalStateException → flash errorMessage → redirect:/approvals

    POST /approvals/{id}/reject
        → rejectRequest(id, reason) → redirect:/approvals
        → IllegalStateException → flash errorMessage → redirect:/approvals/{id}

    GET  /history
        → model: approvedRequests
        → "approvals/history"
}
```

### 4. templates/approvals/list.html（新規）

- 共通レイアウト使用
- PENDING 申請を表として表示: 申請日、金額、申請者（childUserId）
- 各行に「詳細を見る」リンク → /approvals/{id}
- 申請がない場合は空状態メッセージ
- 申請完了バナー（`?approved=true` / `?rejected=true`）

### 5. templates/approvals/detail.html（新規）

- 共通レイアウト使用
- 申請情報: 申請日、金額
- お手伝い明細: 日付・内容の一覧（`<table>`）
- 承認フォーム: `POST /approvals/{id}/approve`
- 却下フォーム: 却下理由 `<textarea>` + `POST /approvals/{id}/reject`
- エラーメッセージ表示

### 6. templates/approvals/history.html（新規）

- 共通レイアウト使用
- APPROVED 申請を表として表示: 承認日時、金額、申請者

### 7. static/css/main.css 追記

- `.approval-table`
- `.approval-detail`
- `.rejection-form`
- `.history-table`

## データフロー

### 申請承認
```
1. POST /approvals/{id}/approve
2. AllowanceService.approveRequest(id)
   a. AllowanceRequest.status = APPROVED, approvedAt = now
   b. Chore.status = APPROVED（全明細）
3. redirect:/approvals?approved=true
```

### 申請却下
```
1. POST /approvals/{id}/reject {reason}
2. AllowanceService.rejectRequest(id, reason)
   a. AllowanceRequest.status = REJECTED, rejectionReason = reason
   b. Chore.status = UNPAID, Chore.requestId = null（全明細）
3. redirect:/approvals?rejected=true
```

## エラーハンドリング

- 存在しない申請ID → `orElseThrow` → IllegalStateException → flash errorMessage → redirect:/approvals
- PENDING 以外の申請を承認/却下しようとした → IllegalStateException → flash errorMessage

## テスト戦略

### AllowanceServiceApprovalTest（JUnit 5 + Mockito）

- `approveRequest` — 正常: AllowanceRequest が APPROVED に、Chore が APPROVED に
- `approveRequest` — 正常: approvedAt がセットされること
- `approveRequest` — 異常: PENDING 以外の申請で `IllegalStateException`
- `rejectRequest` — 正常: AllowanceRequest が REJECTED に、rejectionReason がセットされること
- `rejectRequest` — 正常: Chore が UNPAID に戻り requestId が null になること
- `rejectRequest` — 異常: PENDING 以外の申請で `IllegalStateException`

## ディレクトリ構造

```
src/
├── main/java/design/hitsuji/otetsudai/
│   ├── repository/
│   │   └── ChoreRepository.java      ← findByRequestId 追記
│   ├── service/
│   │   └── AllowanceService.java     ← 承認系メソッド追記
│   └── controller/
│       └── ApprovalController.java   ← 新規
├── main/resources/
│   ├── templates/approvals/
│   │   ├── list.html                 ← 新規
│   │   ├── detail.html               ← 新規
│   │   └── history.html              ← 新規
│   └── static/css/
│       └── main.css                  ← 承認画面スタイル追記
└── test/java/design/hitsuji/otetsudai/
    └── service/
        └── AllowanceServiceApprovalTest.java ← 新規
```

## セキュリティ考慮事項

- `/approvals/**` と `/history` は SecurityConfig で `hasRole("PARENT")` 制限済み（既存設定）
- 申請 ID の操作は Service 層で AllowanceRequest の存在確認と status 確認を行う
- 他家族の申請へのアクセス防止は MVP 段階では家族が1つのみのため省略

## 実装順序

1. ChoreRepository に `findByRequestId` 追記
2. AllowanceService に承認系メソッド追記
3. ApprovalController 新規作成
4. templates/approvals/list.html 新規作成
5. templates/approvals/detail.html 新規作成
6. templates/approvals/history.html 新規作成
7. main.css 追記
8. AllowanceServiceApprovalTest 新規作成
