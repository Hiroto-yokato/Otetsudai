# 設計書

## アーキテクチャ概要

お手伝い登録機能と同じレイヤードアーキテクチャ。AJAX 金額プレビューのみ JSON レスポンスを返す。

```
Browser
  ↓ GET /requests/new
RequestController  ← CHILD ロールのみアクセス可
  ↓ listUnpaidChores()
ChoreService       ← 既存サービスに listUnpaidChores を追加

Browser
  ↓ POST /requests/preview (AJAX, JSON)
RequestController  → AllowanceService.previewAmount() → JSON { "amount": 120 }

Browser
  ↓ POST /requests (フォーム送信)
RequestController  → AllowanceService.createRequest()
  ↓                  ├─ AllowanceRequest 作成 (PENDING)
AllowanceService    └─ Chore.status UNPAID→PENDING, Chore.requestId セット
  ↓
AllowanceRequestRepository + ChoreRepository
  ↓
SQLite (allowance_requests, chores テーブル)
```

## コンポーネント設計

### 1. RequestStatus.java（enums パッケージ）

```java
enum RequestStatus { PENDING, APPROVED, REJECTED }
```

### 2. AllowanceRequest.java（entity パッケージ）

| フィールド | 型 | 制約 |
|-----------|----|----|
| id | Long | PK, AUTO |
| childUserId | Long | FK → users.id, NOT NULL |
| requestDate | LocalDate | NOT NULL |
| totalAmount | Integer | NOT NULL |
| status | RequestStatus | NOT NULL, DEFAULT PENDING |
| rejectionReason | String | nullable |
| approvedAt | LocalDateTime | nullable |
| createdAt | LocalDateTime | NOT NULL |
| updatedAt | LocalDateTime | NOT NULL |

`@PrePersist` / `@PreUpdate` パターンは User / Chore と同一。

### 3. AllowanceRequestRepository.java（repository パッケージ）

```java
interface AllowanceRequestRepository extends JpaRepository<AllowanceRequest, Long> {
    List<AllowanceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    List<AllowanceRequest> findByChildUserIdAndStatus(Long childUserId, RequestStatus status);
}
```

### 4. ChoreRepository.java 追記

```java
// 既存に追加
List<Chore> findByIdInAndUserId(List<Long> ids, Long userId);
```

注: `findByIdInAndStatus` の代わりに `findByIdInAndUserId` を使い、取得後に Service 層でステータスを検証する。

### 5. ChoreService.java 追記

```java
// 既存 ChoreService に追加
@Transactional(readOnly = true)
public List<Chore> listUnpaidChores(String loginUserId) {
    User user = findUserByLoginId(loginUserId);
    return choreRepository.findByUserIdAndStatus(user.getId(), ChoreStatus.UNPAID);
}
```

日付順はテンプレート側で `th:each` 自然順（DBからの返却順）を使用。

### 6. AllowanceService.java（service パッケージ）

**calculateAmount（金額計算ロジック）**:
```
日付ごとにグループ化: Map<LocalDate, Long> countByDate
n = 1: ¥20 / n = 2: ¥50 / n >= 3: ¥50 × (n - 1)
全日付の合計を返す
```

**previewAmount**:
- loginUserId からユーザー取得
- choreIds が空 → 0 を返す
- `choreRepository.findByIdInAndUserId(choreIds, user.getId())` で取得
- `calculateAmount` を呼ぶ

**createRequest**:
1. choreIds が空 → `IllegalStateException("お手伝いを1件以上選択してください")`
2. `choreRepository.findByIdInAndUserId(choreIds, user.getId())` で取得
3. 各 Chore の status が UNPAID か確認 → 違う場合は `IllegalStateException`
4. `calculateAmount(chores)` で金額計算
5. `AllowanceRequest` 作成・保存（status = PENDING）
6. 各 Chore の status = PENDING、requestId = AllowanceRequest.id に更新
7. AllowanceRequest を返す

### 7. RequestController.java（controller パッケージ）

```java
@Controller
@RequestMapping("/requests")
public class RequestController {

    GET  /requests/new
        → model: unpaidChores, hasUnpaid
        → "request/new"

    POST /requests/preview  (@ResponseBody, JSON)
        → @RequestParam List<Long> choreIds (form data)
        → return Map.of("amount", previewAmount(...))

    POST /requests
        → @RequestParam List<Long> choreIds
        → createRequest() → redirect:/chores?applied=true
        → IllegalStateException → flash errorMessage → redirect:/requests/new
}
```

preview は `@RequestParam(required = false) List<Long> choreIds` でフォームデータを受け取る（`application/x-www-form-urlencoded`）。

### 8. chore/list.html 更新

- `?applied=true` のとき申請完了バナーを表示（既存の `?registered=true` バナーと同パターン）
- UNPAID のお手伝いが存在する場合は「お小遣いを申請する」ボタンを表示

### 9. templates/request/new.html（新規）

- 共通レイアウト（`layout :: head`, `layout :: nav`）
- UNPAID お手伝い一覧（チェックボックス）
- 金額プレビューエリア（初期値: 0円）
- JavaScript で checkbox change → fetch POST /requests/preview → 金額更新
- 「申請する」ボタン（form POST /requests）

## データフロー

### 申請フォーム表示
```
1. GET /requests/new
2. ChoreService.listUnpaidChores(userId)
3. UNPAID チョア一覧を "request/new.html" に渡す
4. 初期表示: 全件チェック済み、金額プレビュー表示
```

### AJAX プレビュー
```
1. チェックボックス変更
2. JS: 選択済み choreId を収集
3. fetch POST /requests/preview {choreIds: [1,2,3]}
4. AllowanceService.previewAmount() → 金額計算
5. JSON { "amount": 70 } を返す
6. JS: 金額プレビューエリアを更新
```

### 申請確定
```
1. POST /requests {choreIds: [1,2,3]}
2. AllowanceService.createRequest()
   a. AllowanceRequest(PENDING) を保存
   b. Chore.status = PENDING, Chore.requestId = AllowanceRequest.id に更新
3. redirect:/chores?applied=true
4. chore/list.html に申請完了バナー表示
```

## エラーハンドリング

- `IllegalStateException` を `RedirectAttributes.addFlashAttribute("errorMessage", ...)` でフラッシュ
- preview AJAX は try-catch で 0 を返す（UI は変更しない）

## テスト戦略

### AllowanceServiceTest（JUnit 5 + Mockito）

- `calculateAmount` — 1件(¥20), 2件(¥50), 3件(¥100), 複数日合算
- `createRequest` — 正常: AllowanceRequest が PENDING で保存、Chore が PENDING に更新
- `createRequest` — 異常: 空リストで `IllegalStateException`
- `createRequest` — 異常: 他ユーザーのお手伝い ID で `IllegalStateException`

## ディレクトリ構造

```
src/
├── main/java/design/hitsuji/otetsudai/
│   ├── enums/
│   │   └── RequestStatus.java          ← 新規
│   ├── entity/
│   │   └── AllowanceRequest.java       ← 新規
│   ├── repository/
│   │   ├── ChoreRepository.java        ← findByIdInAndUserId 追記
│   │   └── AllowanceRequestRepository.java ← 新規
│   ├── service/
│   │   ├── ChoreService.java           ← listUnpaidChores 追記
│   │   └── AllowanceService.java       ← 新規
│   └── controller/
│       └── RequestController.java      ← 新規
├── main/resources/
│   ├── templates/
│   │   ├── chore/list.html             ← ?applied=true バナー・申請ボタン追記
│   │   └── request/
│   │       └── new.html               ← 新規
│   └── static/css/
│       └── main.css                   ← 申請フォーム関連スタイル追記
└── test/java/design/hitsuji/otetsudai/
    └── service/
        └── AllowanceServiceTest.java   ← 新規
```

## 実装の順序

1. RequestStatus.java
2. AllowanceRequest.java
3. AllowanceRequestRepository.java
4. ChoreRepository.java に `findByIdInAndUserId` 追記
5. ChoreService.java に `listUnpaidChores` 追記
6. AllowanceService.java
7. RequestController.java
8. chore/list.html 更新
9. request/new.html
10. main.css 追記
11. AllowanceServiceTest.java

## セキュリティ考慮事項

- `/requests/**` は SecurityConfig で `hasRole("CHILD")` 制限済み（既存設定）
- Service 層で `findByIdInAndUserId` により他ユーザーの Chore への操作を防止
- AJAX preview も認証済みユーザーの Chore のみ計算

## パフォーマンス考慮事項

- `findByIdInAndUserId` は IN 句を使うため1クエリで取得可能
- 申請フォームのチェックボックス変更ごとに AJAX が発火するが、ローカル計算のため低レイテンシ
