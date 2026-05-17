# タスクリスト

## 🚨 タスク完全完了の原則

**このファイルの全タスクが完了するまで作業を継続すること**

### 必須ルール
- **全てのタスクを`[x]`にすること**
- 「時間の都合により別タスクとして実施予定」は禁止
- 「実装が複雑すぎるため後回し」は禁止
- 未完了タスク（`[ ]`）を残したまま作業を終了しない

---

## フェーズ1: エンティティ・リポジトリ層

- [x] `User.java` に `displayName` フィールドを追加する
  - [x] `@Column(name = "display_name", length = 50)` アノテーション付きフィールド追加
  - [x] getter / setter 追加
- [x] `UserRepository.java` にメソッドを追記する
  - [x] `findByRoleAndFamilyId(Role role, Long familyId)`
  - [x] `existsByUserId(String userId)`
  - [x] `existsByEmail(String email)`

## フェーズ2: DTO 層

- [x] `UserRegistrationForm.java` を新規作成する
  - [x] `userId` フィールド（@NotBlank, @Size(min=3, max=20), @Pattern）
  - [x] `password` フィールド（@NotBlank, @Size(min=8, max=100)）
  - [x] `passwordConfirm` フィールド（@NotBlank）
  - [x] `email` フィールド（@Email, @Size(max=200), 任意）
  - [x] `displayName` フィールド（@Size(max=50), 任意）
  - [x] getter / setter 追加

## フェーズ3: サービス層

- [x] `UserService.java` を新規作成する
  - [x] `registerChild(String loginUserId, UserRegistrationForm form)` — 登録処理
    - [x] 親ユーザー取得
    - [x] userId 重複確認（existsByUserId）
    - [x] パスワード一致確認
    - [x] email 重複確認（設定時のみ）
    - [x] BCrypt ハッシュ化
    - [x] CHILD ユーザー保存
  - [x] `listChildren(String loginUserId)` — 子ども一覧取得

## フェーズ4: コントローラー層

- [x] `UserController.java` を新規作成する
  - [x] `GET /users` — 子ども一覧
  - [x] `GET /users/new` — 登録フォーム表示
  - [x] `POST /users` — 登録処理
    - [x] Bean Validation エラー → "users/new" に戻る
    - [x] IllegalStateException → errorMessage → "users/new" に戻る
    - [x] 成功 → redirect:/users?registered=true

## フェーズ5: セキュリティ設定

- [x] `SecurityConfig.java` を更新する
  - [x] `/users/**` を PARENT ロール限定に追加

## フェーズ6: ビュー層

- [x] `templates/users/` ディレクトリを作成する
- [x] `templates/users/list.html` を作成する
  - [x] 共通レイアウトを使用する
  - [x] 子どもアカウント一覧テーブル（userId・表示名・登録日）
  - [x] 「子どもを追加する」ボタン → /users/new
  - [x] `?registered=true` → 登録完了バナー
  - [x] 空状態メッセージ
- [x] `templates/users/new.html` を作成する
  - [x] 共通レイアウトを使用する
  - [x] エラーメッセージ表示（サービス層エラー）
  - [x] フォームフィールド（userId, password, passwordConfirm, email, displayName）
  - [x] バリデーションエラー表示（フィールド別 th:errors）
  - [x] 「登録する」/「キャンセル」ボタン
- [x] `static/css/main.css` に users/ 画面のスタイルを追記する

## フェーズ7: テスト

- [x] `UserServiceTest.java` を新規作成する
  - [x] `registerChild_success` — CHILD ユーザーが正しいフィールドで保存されること
  - [x] `registerChild_duplicateUserId` — IllegalStateException が発生すること
  - [x] `registerChild_duplicateEmail` — IllegalStateException が発生すること
  - [x] `registerChild_passwordMismatch` — IllegalStateException が発生すること
  - [x] `listChildren_returnsChildrenOfSameFamily` — CHILD かつ同一 familyId のみ返ること

## フェーズ8: ビルド確認

- [x] `mvn compile` が通ることを確認する
- [x] `mvn test` が通ることを確認する（全39テストパス）
- [x] `mvn spring-boot:run` で起動し、`/users` が表示されることを確認する

---

## 実装後の振り返り

### 実装完了日
2026-05-17

### 計画と実績の差分

**計画と異なった点**:
- `@Pattern` の正規表現にアンカー `^...$` を追加（バリデーター指摘: 意図の明示化）
- `listChildren` に `familyId == null` ガードを追加（バリデーター指摘: 将来の安全性）
- テストを計画の5件 → 8件に強化（parentNotFound x2、email/displayName なし正常系、verify(passwordEncoder) 追加）

**新たに必要になったタスク**:
- `UserService.listChildren` に null ガード追加
- `UserServiceTest` に `registerChild_parentNotFound`, `listChildren_parentNotFound`, `registerChild_withoutEmailAndDisplayName` テスト追加

### 学んだこと

**技術的な学び**:
- Jakarta Validation の `@Pattern` は内部で `Matcher.matches()` を使うため全体一致だが、アンカーを明記することで意図が明確になる
- `StringUtils.hasText()` は空文字・スペースのみの文字列を `false` として扱うため、任意フィールドの null 変換に適している
- `@ModelAttribute("form")` のバインディングエラーパスでは Spring MVC が暗黙的にモデルにオブジェクトを保持するため、ChoreController と同様にフォームに戻る処理が成立する

**プロセス上の改善点**:
- バリデーターが `familyId == null` ガードを指摘。将来のマルチファミリー対応を見越した防御的実装の重要性を再確認

### 次回への改善提案
- マルチファミリー対応時は `UserRepository.findByRoleAndFamilyId` に対するテストをリポジトリ層でも実施する
- 子どもアカウント削除・編集は今回のスコープ外（将来の機能追加時に検討）
- 統合テスト（@SpringBootTest）で PARENT ログイン → /users/new → POST → /users の一連フローを追加する
