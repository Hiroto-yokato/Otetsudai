# リポジトリ構造定義書 (Repository Structure Document)

## プロジェクト構造

```
otetsudai/
├── src/
│   └── main/
│       ├── java/
│       │   └── design/hitsuji/otetsudai/
│       │       ├── controller/        # Presentationレイヤー（Spring MVC Controller）
│       │       ├── service/           # Serviceレイヤー（ビジネスロジック）
│       │       ├── repository/        # Repositoryレイヤー（データアクセス）
│       │       ├── entity/            # JPAエンティティ（DBマッピング）
│       │       ├── dto/               # データ転送オブジェクト（Controller↔Service間）
│       │       ├── enums/             # 列挙型（Role, ChoreStatus, RequestStatus）
│       │       ├── config/            # Spring設定クラス（Security設定等）
│       │       └── OtetsudaiApplication.java  # エントリーポイント
│       └── resources/
│           ├── templates/             # Thymeleafテンプレート（HTML）
│           │   ├── auth/              # 認証関連画面
│           │   ├── chores/            # お手伝い関連画面
│           │   ├── requests/          # お小遣い申請関連画面
│           │   ├── approvals/         # 承認関連画面
│           │   └── fragments/         # 共通レイアウトパーツ
│           ├── static/                # 静的ファイル（CSS, JS, 画像）
│           │   ├── css/
│           │   └── js/
│           └── application.properties # アプリケーション設定
├── src/
│   └── test/
│       └── java/
│           └── design/hitsuji/otetsudai/
│               ├── service/           # Serviceユニットテスト
│               └── integration/       # 統合テスト
├── app-data/                          # SQLiteデータファイル（gitignore対象）
│   └── otetsudai.db
├── docs/                              # プロジェクトドキュメント
├── .steering/                         # 作業単位のステアリングファイル
├── .claude/                           # Claude Code設定
├── pom.xml                            # Mavenビルド定義
└── CLAUDE.md
```

---

## ディレクトリ詳細

### controller/（Presentationレイヤー）

**役割**: HTTPリクエストの受付・入力バリデーション・ThymeleafテンプレートへのModelセット・リダイレクト

**配置ファイル**:
- `AuthController.java`: ログイン・ログアウト処理
- `ChoreController.java`: お手伝い一覧・登録・削除
- `RequestController.java`: お小遣い申請フォーム・申請確定
- `ApprovalController.java`: 申請一覧・詳細・承認・却下・支給履歴

**命名規則**:
- クラス名: `[機能名]Controller.java`（PascalCase + `Controller` 接尾辞）
- HTTPメソッドに対応するメソッド名: `showXxxForm()`, `handleXxx()`, `listXxx()`

**依存関係**:
- 依存可能: `service/`, `dto/`
- 依存禁止: `repository/`（Serviceを通じてのみアクセス）

---

### service/（Serviceレイヤー）

**役割**: ビジネスロジックの実装・トランザクション制御・複数Repositoryの協調

**配置ファイル**:
- `ChoreService.java`: お手伝い登録・削除・一覧取得
- `AllowanceService.java`: お小遣い申請・計算・承認・却下・履歴取得
- `UserDetailsServiceImpl.java`: Spring Security用ユーザー認証ロジック

**命名規則**:
- クラス名: `[機能名]Service.java`（PascalCase + `Service` 接尾辞）
- メソッド名: 動詞+名詞（`createChore`, `approveRequest`, `calculateAmount`）

**依存関係**:
- 依存可能: `repository/`, `entity/`, `dto/`, `enums/`
- 依存禁止: `controller/`、`HttpServletRequest` 等のWeb依存クラス

---

### repository/（Repositoryレイヤー）

**役割**: Spring Data JPA を用いた SQLite への CRUD 操作・カスタムクエリ

**配置ファイル**:
- `UserRepository.java`: ユーザー検索（userId/email）
- `ChoreRepository.java`: お手伝い検索（ステータス・ユーザー・日付）
- `AllowanceRequestRepository.java`: 申請検索（ステータス・子どもID）

**命名規則**:
- クラス名: `[エンティティ名]Repository.java`（interface として定義）
- メソッド名: Spring Data JPA 命名規則（`findBy[Field]`, `findBy[Field]And[Field]`）

**依存関係**:
- 依存可能: `entity/`, `enums/`
- 依存禁止: `service/`, `controller/`

---

### entity/（JPAエンティティ）

**役割**: データベーステーブルとのマッピング定義（`@Entity` クラス）

**配置ファイル**:
- `User.java`: ユーザーエンティティ
- `Chore.java`: お手伝いエンティティ
- `AllowanceRequest.java`: お小遣い申請エンティティ

**命名規則**:
- クラス名: 単数形 PascalCase（`User`, `Chore`, `AllowanceRequest`）
- テーブル名: スネークケース（`@Table(name = "allowance_requests")`）

---

### dto/（データ転送オブジェクト）

**役割**: Controller ↔ Service 間のデータ受け渡し、フォームバインディング

**配置ファイル**:
- `ChoreForm.java`: お手伝い登録フォームの入力モデル
- `RequestForm.java`: お小遣い申請フォームの入力モデル
- `ApprovalForm.java`: 承認・却下フォームの入力モデル

**命名規則**:
- フォーム入力: `[機能名]Form.java`
- 表示用DTO: `[機能名]Dto.java`（必要になった場合）

---

### enums/（列挙型）

**役割**: ドメイン固有の状態・種別を型安全に表現

**配置ファイル**:
- `Role.java`: `PARENT`, `CHILD`
- `ChoreStatus.java`: `UNPAID`, `PENDING`, `APPROVED`, `REJECTED`
- `RequestStatus.java`: `PENDING`, `APPROVED`, `REJECTED`

---

### config/（Spring設定クラス）

**役割**: Spring Boot のビーン定義・セキュリティ設定

**配置ファイル**:
- `SecurityConfig.java`: Spring Security の認証・認可設定（ロール別URLアクセス制御、フォームログイン設定）

---

### templates/（Thymeleafテンプレート）

**役割**: サーバーサイドレンダリング用HTMLテンプレート

```
templates/
├── auth/
│   └── login.html              # ログイン画面
├── chores/
│   ├── list.html               # お手伝い一覧（子どもトップ）
│   └── form.html               # お手伝い登録フォーム
├── requests/
│   └── form.html               # お小遣い申請フォーム
├── approvals/
│   ├── list.html               # 申請一覧（親トップ）
│   ├── detail.html             # 申請詳細・承認
│   └── history.html            # 支給履歴
└── fragments/
    ├── layout.html             # 共通レイアウト（header/footer）
    └── messages.html           # フラッシュメッセージ・アニメーション
```

**命名規則**: kebab-case の HTML ファイル名

---

### static/（静的ファイル）

```
static/
├── css/
│   ├── main.css               # 共通スタイル
│   └── animation.css          # お手伝い登録完了アニメーション
└── js/
    └── preview.js             # 申請金額プレビュー（AJAX）
```

---

## ファイル配置規則

### ソースファイル

| ファイル種別 | 配置先 | 命名規則 | 例 |
|------------|--------|---------|-----|
| Controllerクラス | `controller/` | `[機能]Controller.java` | `ChoreController.java` |
| Serviceクラス | `service/` | `[機能]Service.java` | `AllowanceService.java` |
| Repositoryインターフェース | `repository/` | `[Entity]Repository.java` | `ChoreRepository.java` |
| JPAエンティティ | `entity/` | 単数形PascalCase | `AllowanceRequest.java` |
| フォームDTO | `dto/` | `[機能]Form.java` | `ChoreForm.java` |
| 列挙型 | `enums/` | PascalCase | `ChoreStatus.java` |
| 設定クラス | `config/` | `[機能]Config.java` | `SecurityConfig.java` |

### テストファイル

| テスト種別 | 配置先 | 命名規則 | 例 |
|-----------|--------|---------|-----|
| ユニットテスト（Service） | `test/java/.../service/` | `[対象]Test.java` | `AllowanceServiceTest.java` |
| 統合テスト | `test/java/.../integration/` | `[シナリオ]IT.java` | `ChoreApprovalFlowIT.java` |

---

## 依存関係のルール

```
Controller（Presentationレイヤー）
    ↓ 依存OK
Service（Serviceレイヤー）
    ↓ 依存OK
Repository（データレイヤー）
    ↓ 依存OK
Entity / SQLite
```

**禁止される依存**:
- `repository/` → `service/` または `controller/` （❌）
- `service/` → `controller/` （❌）
- `entity/` → `service/`, `controller/`, `repository/` （❌）

---

## 除外設定（.gitignore）

```gitignore
# ビルド成果物
target/

# アプリデータ
app-data/

# IDE
.idea/
*.iml

# OS
.DS_Store
Thumbs.db

# 環境設定（機密情報を含む場合）
application-local.properties
```
