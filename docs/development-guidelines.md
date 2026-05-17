# 開発ガイドライン (Development Guidelines)

## コーディング規約

### 命名規則（Java）

**クラス・インターフェース・列挙型**:
```java
// クラス: PascalCase + 役割接尾辞
class ChoreService { }
class AllowanceRequestRepository { }
class SecurityConfig { }

// インターフェース: PascalCase（I接頭辞不使用）
interface ChoreRepository extends JpaRepository<Chore, Long> { }

// 列挙型: PascalCase
enum ChoreStatus { UNPAID, PENDING, APPROVED, REJECTED }
```

**メソッド・変数**:
```java
// メソッド: camelCase、動詞で始める
public AllowanceRequest createRequest(Long childUserId, List<Long> choreIds) { }
public int calculateAmount(List<Chore> chores) { }
public void approveRequest(Long requestId) { }

// 変数: camelCase、名詞
Long childUserId;
List<Chore> unpaidChores;
int totalAmount;

// Boolean: is/has/can で始める
boolean isApproved;
boolean hasUnpaidChores;
```

**定数**:
```java
// UPPER_SNAKE_CASE
private static final int SINGLE_CHORE_AMOUNT = 20;
private static final int DOUBLE_CHORE_AMOUNT = 50;
private static final int ADDITIONAL_CHORE_AMOUNT = 50;
```

**パッケージ**:
```
design.hitsuji.otetsudai.controller
design.hitsuji.otetsudai.service
design.hitsuji.otetsudai.repository
design.hitsuji.otetsudai.entity
design.hitsuji.otetsudai.dto
design.hitsuji.otetsudai.enums
design.hitsuji.otetsudai.config
```

---

### コードフォーマット

- **インデント**: 4スペース（タブ不使用）
- **行の最大長**: 120文字
- **中括弧**: K&Rスタイル（同じ行に開き括弧）

```java
// ✅ 良い例
public Chore createChore(Long userId, LocalDate choreDate, String content) {
    Chore chore = new Chore();
    chore.setUserId(userId);
    chore.setChoreDate(choreDate);
    chore.setContent(content);
    chore.setStatus(ChoreStatus.UNPAID);
    return choreRepository.save(chore);
}

// ❌ 悪い例（インデント不統一、意味のない変数名）
public Chore create(Long u, LocalDate d, String c)
{
    Chore x = new Chore();
    x.setUserId(u);
    return choreRepository.save(x);
}
```

---

### コメント規約

**Javadocコメント**: ビジネスロジックの非自明な部分にのみ記述する

```java
/**
 * お小遣い計算ルール: 1日ごとにお手伝い回数を集計し合算する
 * - 1回: ¥20、2回: ¥50、n回(n≥3): ¥50×(n-1)
 */
public int calculateAmount(List<Chore> chores) {
    // ...
}
```

**インラインコメント**:「なぜ」そうするかを説明する（「何を」はコードから分かる）

```java
// ✅ 良い例: 理由を説明
// 却下時はお手伝いを未申請状態に戻すことで再申請を可能にする
chore.setStatus(ChoreStatus.UNPAID);
chore.setRequestId(null);

// ❌ 悪い例: コードを日本語にしただけ
// ステータスをUNPAIDに設定する
chore.setStatus(ChoreStatus.UNPAID);
```

---

### エラーハンドリング

**Service層**: ビジネスルール違反は `IllegalStateException` または専用例外でスロー

```java
public void deleteChore(Long choreId, Long userId) {
    Chore chore = choreRepository.findById(choreId)
        .orElseThrow(() -> new EntityNotFoundException("お手伝いが見つかりません: " + choreId));

    // 認可チェック: 自分のお手伝いのみ削除可
    if (!chore.getUserId().equals(userId)) {
        throw new AccessDeniedException("このお手伝いを削除する権限がありません");
    }

    // ビジネスルール: 未申請状態のみ削除可
    if (chore.getStatus() != ChoreStatus.UNPAID) {
        throw new IllegalStateException("申請中または承認済みのお手伝いは削除できません");
    }

    choreRepository.delete(chore);
}
```

**Controller層**: 例外を `@ExceptionHandler` またはグローバルハンドラーで捕捉してユーザーフレンドリーなメッセージに変換

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException e, RedirectAttributes attrs) {
        attrs.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/chores";
    }
}
```

---

### セキュリティ

**入力検証**: Bean Validation アノテーションを DTO に付与し、Controller で `@Valid` 適用

```java
public class ChoreForm {
    @NotBlank(message = "お手伝い内容を入力してください")
    @Size(max = 100, message = "100文字以内で入力してください")
    private String content;

    @NotNull(message = "日付を選択してください")
    @PastOrPresent(message = "未来の日付は登録できません")
    private LocalDate choreDate;
}
```

**認可チェック**: Service層でリソースの所有者確認を必ず実施する（Controller層のロール制御だけに依存しない）

---

## Git運用ルール

### ブランチ戦略（Git Flow）

```
main（本番リリース済み）
└── develop（開発統合）
    ├── feature/[機能名]   例: feature/chore-registration
    ├── fix/[修正内容]     例: fix/amount-calculation
    └── refactor/[対象]   例: refactor/allowance-service
```

**運用ルール**:
- `main` / `develop` への直接コミット禁止（PR経由のみ）
- `feature/*` / `fix/*` は `develop` から分岐し、完了後に `develop` へPR
- `develop` → `main` は squash merge 禁止（merge commit を使用）

### コミットメッセージ規約（Conventional Commits）

```
<type>(<scope>): <subject>

<body>（任意）

<footer>（任意）
```

**Type一覧**:

| type | 用途 |
|------|------|
| `feat` | 新機能 |
| `fix` | バグ修正 |
| `docs` | ドキュメント |
| `refactor` | リファクタリング |
| `test` | テスト追加・修正 |
| `chore` | ビルド設定・依存関係更新 |

**例**:
```
feat(chore): お手伝い登録完了アニメーションを追加

登録完了後に「今日もお手伝いありがとう！」と
スパークルアニメーションを3秒表示する。

Closes #12
```

### プルリクエストのプロセス

**PR作成前チェックリスト**:
- [ ] `mvn test` が全パス
- [ ] `mvn compile` でコンパイルエラーなし
- [ ] セルフレビュー実施済み

**PRテンプレート**:
```markdown
## 変更の種類
- [ ] 新機能 (feat)
- [ ] バグ修正 (fix)
- [ ] リファクタリング (refactor)
- [ ] ドキュメント (docs)

## 変更内容
### 何を変更したか
[簡潔な説明]

### なぜ変更したか
[背景・理由]

## テスト
- [ ] ユニットテスト追加・更新
- [ ] 手動テスト実施（対象画面・操作を記載）

## 関連Issue
Closes #[番号]
```

---

## テスト戦略

### テストピラミッド

```
      /\
     /E2E\          少（手動テスト）
    /------\
   / 統合   \        中（@SpringBootTest）
  /----------\
 / ユニット   \       多（JUnit5 + Mockito）
/--------------\
```

**カバレッジ目標**:
- Service層（ビジネスロジック）: **80%以上**
- Repository層: Spring Data JPA の標準動作はテスト不要、カスタムクエリのみ
- Controller層: 統合テストで主要フローをカバー

### ユニットテスト（JUnit 5 + Mockito）

**構造: Given-When-Then**

```java
@ExtendWith(MockitoExtension.class)
class AllowanceServiceTest {

    @Mock
    private ChoreRepository choreRepository;

    @InjectMocks
    private AllowanceService allowanceService;

    @Test
    void calculateAmount_singleChorePerDay_returns20() {
        // Given
        List<Chore> chores = List.of(
            createChore(LocalDate.of(2026, 5, 1))
        );

        // When
        int amount = allowanceService.calculateAmount(chores);

        // Then
        assertThat(amount).isEqualTo(20);
    }

    @Test
    void calculateAmount_threeChoreSameDay_returns100() {
        // Given: 同じ日に3回お手伝い
        LocalDate date = LocalDate.of(2026, 5, 1);
        List<Chore> chores = List.of(
            createChore(date), createChore(date), createChore(date)
        );

        // When
        int amount = allowanceService.calculateAmount(chores);

        // Then: ¥50 × (3-1) = ¥100
        assertThat(amount).isEqualTo(100);
    }
}
```

**テスト命名規則**: `[メソッド名]_[条件]_[期待結果]`

### 統合テスト（@SpringBootTest）

```java
@SpringBootTest
@Transactional
class ChoreApprovalFlowIT {

    @Autowired
    private AllowanceService allowanceService;

    @Autowired
    private ChoreRepository choreRepository;

    @Test
    void approveRequest_changesChoreStatusToApproved() {
        // Given: お手伝いが登録済み・申請済みの状態
        // When: 親が承認
        // Then: お手伝いのステータスがAPPROVEDになる
    }

    @Test
    void rejectRequest_restoresChoreStatusToUnpaid() {
        // Given: 申請中の状態
        // When: 親が却下
        // Then: お手伝いがUNPAIDに戻り再申請可能
    }
}
```

---

## コードレビュー基準

### レビューポイント

**機能性**:
- [ ] PRDの受け入れ条件を満たしているか
- [ ] お手伝いのステータス遷移が正しいか
- [ ] お小遣い計算ロジックが仕様通りか

**セキュリティ**:
- [ ] 自分のリソース以外を操作できないか（他ユーザーのデータへの不正アクセス）
- [ ] Bean Validationが適切に設定されているか
- [ ] CSRF対策が機能しているか

**可読性**:
- [ ] メソッド名・変数名が日本語ドメインに合っているか
- [ ] ビジネスロジックにコメントがあるか

**レビューコメントの優先度**:
- `[必須]`: マージ前に必ず修正
- `[推奨]`: 修正を推奨、合意なしにマージ可
- `[提案]`: 次のPRで対応でも可
- `[質問]`: 理解のための確認

---

## 開発環境セットアップ

### 必要なツール

| ツール | バージョン | インストール方法 |
|--------|-----------|-----------------|
| Java | 25 | [sdkman.io](https://sdkman.io) または公式インストーラー |
| Maven | 3.9.x | `sdk install maven` または公式インストーラー |
| Git | 最新 | 公式インストーラー |

### セットアップ手順

```bash
# 1. リポジトリのクローン
git clone <URL>
cd otetsudai

# 2. ビルド確認
mvn clean install

# 3. データディレクトリの作成（自動作成される場合は不要）
mkdir -p app-data

# 4. 開発サーバーの起動
mvn spring-boot:run

# 5. ブラウザで確認
# http://localhost:8080/login
```

### 主要な Maven コマンド

```bash
mvn clean install              # 全ビルド + テスト
mvn spring-boot:run            # 開発サーバー起動
mvn test                       # 全テスト実行
mvn test -Dtest=ClassName      # 特定クラスのテスト実行
mvn test -Dtest=Class#method   # 特定メソッドのテスト実行
mvn compile                    # コンパイルのみ
```
