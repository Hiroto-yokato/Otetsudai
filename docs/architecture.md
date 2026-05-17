# 技術仕様書 (Architecture Design Document)

## テクノロジースタック

### 言語・ランタイム

| 技術 | バージョン |
|------|-----------|
| Java | 25 |
| Maven | 3.9.x |

### フレームワーク・ライブラリ

| 技術 | バージョン | 用途 | 選定理由 |
|------|-----------|------|----------|
| Spring Boot | 4.0.6 | Webフレームワーク | DIコンテナ・MVC・Security・JPA が統合済みで、設定量を最小化できる |
| Spring Security | (Spring Boot同梱) | 認証・認可 | フォーム認証・CSRF・ロールベースアクセス制御を標準サポート |
| Thymeleaf | (Spring Boot同梱) | サーバーサイドテンプレート | Spring MVCとの統合が最小コストで実現でき、SPAなしでシンプルなUIを構築できる |
| Spring Data JPA | (Spring Boot同梱) | ORMとリポジトリ抽象化 | SQLの記述量を削減しつつ、型安全なクエリを実現できる |
| SQLite JDBC | 3.x | SQLiteドライバ | 軽量で設定不要のファイルDBとして、家族単位の小規模利用に適している |

### 開発ツール

| 技術 | バージョン | 用途 | 選定理由 |
|------|-----------|------|----------|
| JUnit 5 | (Spring Boot同梱) | ユニット・統合テスト | Spring Boot Test との統合が容易 |
| Mockito | (Spring Boot同梱) | テストモック | サービス層のユニットテストでDI依存を差し替えるため |
| Hibernate Validator | (Spring Boot同梱) | Bean Validation | アノテーションベースの入力検証を Controller/Entity 両方に適用できる |

---

## アーキテクチャパターン

### レイヤードアーキテクチャ

```
┌─────────────────────────────────────────────┐
│  Presentation Layer（Thymeleaf + Controller） │  ← HTTP受付・バリデーション・ビュー返却
├─────────────────────────────────────────────┤
│  Service Layer                               │  ← ビジネスロジック・トランザクション管理
├─────────────────────────────────────────────┤
│  Repository Layer（Spring Data JPA）         │  ← データ永続化・クエリ
├─────────────────────────────────────────────┤
│  SQLite                                      │  ← データストア
└─────────────────────────────────────────────┘
```

#### Presentation Layer（Controller + Thymeleaf）
- **責務**: HTTPリクエストの受付、入力バリデーション、Modelへのデータセット、Thymeleafテンプレート返却
- **許可される操作**: Serviceレイヤーの呼び出し
- **禁止される操作**: Repositoryへの直接アクセス、ビジネスロジックの実装

#### Service Layer
- **責務**: ビジネスロジックの実装、トランザクション制御、複数Repositoryの協調
- **許可される操作**: Repositoryの呼び出し
- **禁止される操作**: HTTPコンテキストへの依存（`HttpServletRequest`等）

#### Repository Layer
- **責務**: エンティティのCRUD操作、カスタムクエリ
- **許可される操作**: SQLiteへのアクセス
- **禁止される操作**: ビジネスロジックの実装

---

## データ永続化戦略

### ストレージ方式

| データ種別 | ストレージ | フォーマット | 理由 |
|-----------|----------|-------------|------|
| ユーザー・お手伝い・申請データ | SQLite | RDB（JPA/Hibernate） | リレーション整合性の保証とトランザクション管理のため |
| セッション | Spring Session（メモリ） | Java オブジェクト | 家族単位の小規模利用でサーバー再起動時のセッション破棄を許容できる |

### SQLiteファイルの配置

```
app-data/
└── otetsudai.db    # SQLiteデータベースファイル
```

`application.properties` で配置先を設定:
```properties
spring.datasource.url=jdbc:sqlite:./app-data/otetsudai.db
```

### バックアップ戦略

- **方式**: SQLiteファイルのコピー
- **頻度**: 手動（MVP段階）
- **保存先**: `app-data/backup/otetsudai-YYYYMMDD.db`
- **将来対応**: 定期的な自動バックアップスクリプトを検討（P1）

### スキーマ管理

Spring Boot の DDL 自動生成（`spring.jpa.hibernate.ddl-auto=update`）を使用し、開発中のスキーマ変更を自動反映する。本番移行後は Flyway 等のマイグレーションツール導入を検討する。

---

## パフォーマンス要件

### レスポンスタイム

| 操作 | 目標時間 | 測定環境 |
|------|---------|---------|
| ページ遷移・画面表示 | 1秒以内 | ローカルPC（Core i5相当、メモリ8GB） |
| お手伝い登録（POST → リダイレクト → 完了画面） | 2秒以内 | 同上 |
| お小遣い申請金額プレビュー（AJAX） | 500ms以内 | 同上 |

### リソース使用量

| リソース | 上限 | 理由 |
|---------|------|------|
| ヒープメモリ | 256MB | 家族単位の小規模利用では十分 |
| SQLiteファイルサイズ | 100MB | 1家族1年分の利用データで十分 |

---

## セキュリティアーキテクチャ

### 認証・認可

- **認証方式**: Spring Security フォーム認証（セッションベース）
- **ログインID**: ユーザーID または メールアドレス のどちらでも認証可
- **パスワード保護**: BCrypt（strength=12）でハッシュ化
- **ロール制御**: `ROLE_PARENT` / `ROLE_CHILD` でエンドポイントアクセスを制御

```
/login              → 全員アクセス可
/chores/**          → ROLE_CHILD のみ
/requests/**        → ROLE_CHILD のみ
/approvals/**       → ROLE_PARENT のみ
/history            → ROLE_PARENT のみ
```

### CSRF対策

Spring Security の CSRF トークンを有効化し、Thymeleaf フォームに自動付与する。

### 入力検証

- **Controller 層**: `@Valid` + Bean Validation で入力値検証
- **Service 層**: 認可チェック（操作対象リソースが自分のものか確認）
- **エラー時**: スタックトレースをレスポンスに含めない（Spring Boot の `server.error.include-stacktrace=never`）

### 機密情報管理

- DBパスワード等の機密情報は `application.properties` に直接記載せず、環境変数で上書き可能な設計にする
- `.gitignore` で `app-data/` および機密設定ファイルを除外

---

## スケーラビリティ設計

### データ増加への対応

- **想定データ量**: 1家族 × 3年 × 365日 × 3回/日 = 約3,300件のお手伝いレコード
- **対策**: 一覧表示はページネーションを実装（1ページ30件）
- **インデックス**: `chore.user_id + chore.status`、`chore.chore_date` にインデックスを作成

### 将来のマルチファミリー対応

- `User` エンティティに `family_id` カラムを初期から持たせ、データの分離境界を定義しておく
- Service 層のクエリは常に `family_id` でフィルタリングする設計にする（MVP段階では全ユーザーが同一家族扱い）

---

## テスト戦略

### ユニットテスト
- **フレームワーク**: JUnit 5 + Mockito
- **対象**: Service 層のビジネスロジック（お小遣い計算、ステータス遷移）
- **カバレッジ目標**: Service 層 80% 以上

### 統合テスト
- **方法**: `@SpringBootTest` + インメモリ SQLite（テスト用プロファイル）
- **対象**: Controller → Service → Repository の連携フロー

### E2Eテスト
- **ツール**: 手動テスト（MVP段階）
- **シナリオ**: 子どもの登録→申請、親の承認→履歴確認

---

## 技術的制約

### 環境要件
- **OS**: Windows / macOS / Linux（Java 25 が動作する環境）
- **JVM**: Java 25 以上
- **最小メモリ**: 512MB（JVMヒープ + OS）
- **必要ディスク容量**: 200MB（JARファイル + SQLiteデータ）

### パフォーマンス制約
- SQLite はファイルロックを使用するため、同時書き込みは直列化される（家族単位の利用では問題なし）
- 将来の外部公開時は PostgreSQL 等への移行を検討する

---

## 依存関係管理

| ライブラリ | 用途 | バージョン管理方針 |
|-----------|------|-------------------|
| Spring Boot | フレームワーク全体 | `4.0.6` に固定（BOM経由で子ライブラリを統一管理） |
| SQLite JDBC | SQLiteドライバ | マイナーバージョンまで許容（`3.x`） |
| Hibernate | JPA実装 | Spring Boot BOM に従う |

`pom.xml` では `spring-boot-starter-parent` を親 POM として使用し、依存ライブラリのバージョン整合性を Spring Boot BOM に委譲する。
