# 設計書

## アーキテクチャ概要

ログイン機能と同じレイヤードアーキテクチャに従う。

```
Browser
  ↓ POST /chores, GET /chores
ChoreController  ← @AuthenticationPrincipal で認証済みユーザー取得
  ↓
ChoreService     ← ビジネスロジック（UNPAID チェック、所有者チェック）
  ↓
ChoreRepository  ← Spring Data JPA
  ↓
SQLite (chores テーブル)
```

## コンポーネント設計

### 1. ChoreStatus.java（enums パッケージ）

**責務**: お手伝いのステータスを表す列挙型

```java
enum ChoreStatus { UNPAID, PENDING, APPROVED, REJECTED }
```

### 2. Chore.java（entity パッケージ）

**責務**: お手伝いの JPA エンティティ

| フィールド | 型 | 制約 |
|-----------|----|----|
| id | Long | PK, AUTO |
| userId | Long | FK → users.id, NOT NULL |
| choreDate | LocalDate | NOT NULL |
| content | String | NOT NULL, 1-100文字 |
| status | ChoreStatus | NOT NULL, DEFAULT UNPAID |
| requestId | Long | nullable（未申請時 null） |
| createdAt | LocalDateTime | NOT NULL, 挿入時セット |
| updatedAt | LocalDateTime | NOT NULL, 更新時セット |

`@PrePersist` / `@PreUpdate` で日時を自動セット（User エンティティと同パターン）。

**実装の要点**:
- `status` は `@Enumerated(EnumType.STRING)` で文字列保存
- `@Column(name = "chore_date")` / `@Column(name = "user_id")` でスネークケースマッピング
- `requestId` は `@Column(name = "request_id")` で nullable

### 3. ChoreForm.java（dto パッケージ）

**責務**: 登録フォームのバインディング DTO

```java
class ChoreForm {
    @NotNull
    @PastOrPresent
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate choreDate;   // デフォルト値: 今日

    @NotBlank
    @Size(min = 1, max = 100)
    String content;
}
```

### 4. ChoreRepository.java（repository パッケージ）

**責務**: Chore エンティティの CRUD

```java
interface ChoreRepository extends JpaRepository<Chore, Long> {
    List<Chore> findByUserIdOrderByChoreDate(Long userId);          // 全件（日付順）
    List<Chore> findByUserIdOrderByChoreDate​Desc(Long userId);      // 日付降順
    List<Chore> findByUserIdAndStatus(Long userId, ChoreStatus s);  // ステータス絞り込み
}
```

実際には `findByUserIdOrderByChoreDate​Desc` を一覧取得に使用。

### 5. ChoreService.java（service パッケージ）

**責務**: ビジネスロジック

```java
class ChoreService {
    // お手伝い一覧取得（日付降順）
    List<Chore> listChores(String loginUserId)

    // お手伝い登録（status = UNPAID で保存）
    Chore createChore(String loginUserId, LocalDate choreDate, String content)

    // お手伝い削除（UNPAID かつ自分のものであることを確認）
    void deleteChore(Long choreId, String loginUserId)
}
```

**deleteChore の検証ロジック**:
1. choreId で Chore を取得（見つからなければ `IllegalStateException`）
2. loginUserId から User を取得
3. `chore.getUserId() != user.getId()` → `IllegalStateException("他ユーザーのお手伝いは削除できません")`
4. `chore.getStatus() != UNPAID` → `IllegalStateException("申請中または承認済みのお手伝いは削除できません")`
5. `choreRepository.delete(chore)`

**注意**: Service レイヤーは `HttpServletRequest` を受け取ってはいけない（architecture.md の制約）。

### 6. ChoreController.java（controller パッケージ）

**責務**: HTTP リクエストの受付・バリデーション・ビュー返却

```java
@Controller
@RequestMapping("/chores")
public class ChoreController {
    GET  /chores         → "chore/list"  (model: chores=listChores())
    GET  /chores/new     → "chore/new"   (model: form=new ChoreForm())
    POST /chores         → バリデーション OK → redirect:/chores?registered=true
                           バリデーション NG → "chore/new"（エラー表示）
    POST /chores/{id}/delete → redirect:/chores
}
```

認証済みユーザーの取得:
```java
@AuthenticationPrincipal UserDetails userDetails
String loginUserId = userDetails.getUsername();  // User.userId の String
```

## データフロー

### お手伝い登録フロー
```
1. GET /chores/new → ChoreForm（choreDate=今日）を Model に設定 → chore/new.html
2. POST /chores → @Valid ChoreForm バリデーション
   - エラーあり → chore/new.html にエラー表示
   - エラーなし → ChoreService.createChore() → redirect:/chores?registered=true
3. GET /chores?registered=true → 一覧 + アニメーション表示
```

### お手伝い削除フロー
```
1. POST /chores/{id}/delete → ChoreService.deleteChore(id, loginUserId)
   - 成功 → redirect:/chores
   - IllegalStateException → エラーメッセージを FlashAttribute で一覧に表示
```

## エラーハンドリング戦略

### ChoreService での例外
- `IllegalStateException` を使用（アーキテクチャ制約: architecture.md §エラーハンドリング）
- Controller で `RedirectAttributes.addFlashAttribute("errorMessage", e.getMessage())` でフラッシュ

### バリデーションエラー
- `BindingResult` で検出 → フォームに戻る（chore/new.html）
- Thymeleaf の `th:errors` でフィールドエラーを表示

## テスト戦略

### ユニットテスト（ChoreServiceTest.java）

- `createChore` → Chore が UNPAID で保存されること
- `deleteChore` → UNPAID の Chore が削除されること
- `deleteChore` → PENDING の Chore は例外が投げられること
- `deleteChore` → 他ユーザーの Chore は例外が投げられること

テストパターン: JUnit 5 + Mockito（UserDetailsServiceImplTest と同じ構成）

## 依存ライブラリ

新規追加なし。既存 pom.xml の依存関係で実装可能:
- `spring-boot-starter-validation`（Bean Validation: `@Valid`, `@NotBlank`, `@Size`, `@PastOrPresent`）
- `spring-boot-starter-web`（`@Controller`, `@ModelAttribute`）

## ディレクトリ構造

```
src/
├── main/java/design/hitsuji/otetsudai/
│   ├── enums/
│   │   └── ChoreStatus.java        ← 新規
│   ├── entity/
│   │   └── Chore.java              ← 新規
│   ├── dto/
│   │   └── ChoreForm.java          ← 新規
│   ├── repository/
│   │   └── ChoreRepository.java    ← 新規
│   ├── service/
│   │   └── ChoreService.java       ← 新規
│   └── controller/
│       └── ChoreController.java    ← 新規
├── main/resources/
│   ├── templates/chore/
│   │   ├── list.html               ← 新規
│   │   └── new.html                ← 新規
│   └── static/css/
│       └── main.css                ← 追記（お手伝い一覧・登録フォーム・アニメーション）
└── test/java/design/hitsuji/otetsudai/
    └── service/
        └── ChoreServiceTest.java   ← 新規
```

## 実装の順序

1. `ChoreStatus.java`（enum）
2. `Chore.java`（entity）
3. `ChoreForm.java`（DTO）
4. `ChoreRepository.java`（repository）
5. `ChoreService.java`（service）
6. `ChoreController.java`（controller）
7. `chore/list.html`（Thymeleaf テンプレート）
8. `chore/new.html`（Thymeleaf テンプレート）
9. `main.css` 追記（お手伝い関連スタイル）
10. `ChoreServiceTest.java`（ユニットテスト）

## セキュリティ考慮事項

- `/chores/**` は SecurityConfig で `hasRole("CHILD")` 制限済み（既存設定）
- Service 層で `userId` 一致チェックを行い、他ユーザーのリソースへのアクセスを防止
- バリデーションは Controller の `@Valid` と Bean Validation で二重に実施

## パフォーマンス考慮事項

- お手伝い一覧は `findByUserIdOrderByChoreDate​Desc` で日付降順で取得（最新が先頭）
- MVPではページネーションなし（1家族の利用で十分）
- `chore.user_id + chore.status` インデックスは将来の申請機能で追加予定

## 将来の拡張性

- `requestId` フィールドを初期から持たせることで、お小遣い申請機能（次フェーズ）との連携を準備
- `ChoreStatus.REJECTED` 後の UNPAID 戻し処理は申請機能と同時に実装
