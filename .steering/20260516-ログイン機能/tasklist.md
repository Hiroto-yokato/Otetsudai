# タスクリスト

## 🚨 タスク完全完了の原則

**このファイルの全タスクが完了するまで作業を継続すること**

### 必須ルール
- **全てのタスクを`[x]`にすること**
- 「時間の都合により別タスクとして実施予定」は禁止
- 「実装が複雑すぎるため後回し」は禁止
- 未完了タスク（`[ ]`）を残したまま作業を終了しない

---

## フェーズ1: プロジェクト基盤構築

- [x] pom.xml を作成する（Spring Boot, Security, Thymeleaf, JPA, SQLite の依存関係）
- [x] application.properties を作成する（SQLite 接続設定、JPA DDL 設定）
- [x] app-data/ ディレクトリを作成する
- [x] .gitignore を作成する（app-data/, target/, .idea/, *.iml を除外）
- [x] OtetsudaiApplication.java を作成する

## フェーズ2: ドメイン層（Entity・Repository・Enum）

- [x] Role.java（列挙型）を作成する
- [x] User.java（JPA エンティティ）を作成する
- [x] UserRepository.java（Spring Data JPA インターフェース）を作成する

## フェーズ3: セキュリティ層

- [x] UserDetailsServiceImpl.java を作成する（userId または email でユーザー検索）
- [x] AuthSuccessHandler.java を作成する（ロール別リダイレクト）
- [x] SecurityConfig.java を作成する（フィルターチェーン、認証・認可設定）

## フェーズ4: Web層（Controller・テンプレート）

- [x] AuthController.java を作成する（GET /login）
- [x] templates/fragments/layout.html を作成する（共通レイアウト）
- [x] templates/auth/login.html を作成する（ログインフォーム）
- [x] static/css/main.css を作成する（最小限のスタイル）

## フェーズ5: 初期データ投入

- [x] DataInitializer.java を作成する（開発用サンプルデータ投入）

## フェーズ6: テスト

- [x] UserDetailsServiceImplTest.java を作成する
  - [x] userId でログインできること
  - [x] email でログインできること
  - [x] 存在しないユーザーで UsernameNotFoundException が投げられること

## フェーズ7: ビルド確認

- [x] `mvn compile` が通ることを確認する
- [x] `mvn test` が通ることを確認する
- [x] `mvn spring-boot:run` で起動し、ブラウザで /login が表示されることを確認する

---

## 実装後の振り返り

### 実装完了日
2026-05-16

### 計画と実績の差分

**計画と異なった点**:
- Spring Boot 4.0.6 / Java 25 はリリース前のため、Spring Boot 3.4.5 / Java 21（LTS）で実装した。機能的な差異はなし
- `mvn spring-boot:run` 起動時、Maven の PATH が未設定で環境変数の再読み込みが必要だった

**新たに必要になったタスク**:
- なし（計画通りに完了）

### 学んだこと

**技術的な学び**:
- SQLite + Hibernate を使うには `hibernate-community-dialects` が必要（`SQLiteDialect` がコミュニティ管理）
- userId・email の OR 検索は `findByUserIdOrEmail(loginId, loginId)` のように同値を両引数に渡すことで実現可能
- ロール別リダイレクトは `defaultSuccessUrl` ではなく `AuthenticationSuccessHandler` をカスタム実装する

**プロセス上の改善点**:
- Java/Maven のバージョン確認を設計フェーズで行い、pom.xml に正確なバージョンを記載すべき

### 次回への改善提案
- `pom.xml` の Java バージョンを CLAUDE.md の技術スタックと一致させる（現在は Java 25 が利用可能になり次第、移行が必要）
- Mockito の Java agent 警告（JDK 将来バージョンで self-attach 非推奨）への対応を検討する
