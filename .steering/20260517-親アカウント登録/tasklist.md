# タスクリスト

## 🚨 タスク完全完了の原則

**このファイルの全タスクが完了するまで作業を継続すること**

---

## フェーズ1: DTO 層

- [x] `ParentRegistrationForm.java` を新規作成する
  - [x] `userId`（@NotBlank, @Size(min=3,max=20), @Pattern）
  - [x] `password`（@NotBlank, @Size(min=8,max=100)）
  - [x] `passwordConfirm`（@NotBlank）
  - [x] `email`（@NotBlank, @Email, @Size(max=200)）— 親は必須
  - [x] `displayName`（@Size(max=50)）— 任意
  - [x] getter / setter 追加

## フェーズ2: サービス層

- [x] `UserService.java` に `registerParent(ParentRegistrationForm form)` を追加する
  - [x] userId 重複確認
  - [x] パスワード一致確認
  - [x] email 重複確認
  - [x] BCrypt ハッシュ化
  - [x] PARENT ユーザー保存（1回目）
  - [x] familyId = 保存後の id をセットして再保存（2回目）

## フェーズ3: コントローラー層

- [x] `AuthController.java` に登録エンドポイントを追加する
  - [x] `GET /register` — フォーム表示
  - [x] `POST /register` — 登録処理
    - [x] Bean Validation エラー → "auth/register" に戻る
    - [x] IllegalStateException → errorMessage → "auth/register" に戻る
    - [x] 成功 → redirect:/login?registered=true

## フェーズ4: セキュリティ設定

- [x] `SecurityConfig.java` の permitAll に `/register` を追加する

## フェーズ5: ビュー層

- [x] `templates/auth/register.html` を新規作成する
  - [x] ログインページと同様のカード型レイアウト
  - [x] エラーメッセージ表示
  - [x] フォームフィールド（userId, password, passwordConfirm, email, displayName）
  - [x] 「登録する」ボタン
  - [x] 「ログインに戻る」リンク
- [x] `templates/auth/login.html` に登録ページへのリンクを追加する
  - [x] `?registered=true` → 登録完了メッセージを表示する
  - [x] 「アカウントをお持ちでない方はこちら」→ /register リンクを追加する
- [x] `static/css/main.css` に register ページのスタイルを追記する

## フェーズ6: テスト

- [x] `UserServiceTest.java` に `registerParent` テストを追加する
  - [x] `registerParent_success` — PARENT ユーザーが保存され familyId = id になること
  - [x] `registerParent_duplicateUserId` — IllegalStateException が発生すること
  - [x] `registerParent_duplicateEmail` — IllegalStateException が発生すること
  - [x] `registerParent_passwordMismatch` — IllegalStateException が発生すること

## フェーズ7: ビルド確認

- [x] `mvn compile` が通ることを確認する
- [x] `mvn test` が通ることを確認する（全46テストパス）

---

## 実装後の振り返り

### 実装完了日
2026-05-17

### 計画と実績の差分

**計画と異なった点**:
- 特になし。計画通りに実装完了。

**新たに必要になったタスク**:
- `UserServiceTest` に `setId` リフレクションヘルパーを追加（familyId = id の検証に必要）

### 学んだこと

**技術的な学び**:
- familyId 自動生成は「保存後の ID を familyId にセットして再保存」の 2-save パターンで実現。シーケンステーブルや UUID 不要でシンプル
- `@NotBlank` は空文字だけでなく null も拒否するため、親フォームの email 必須化に適切
- `when(repo.save(any())).thenReturn(first).thenReturn(second)` で複数回呼び出しの返り値を順番に指定できる

**プロセス上の改善点**:
- 特になし

### 次回への改善提案
- メール確認（email verification）が将来必要になった場合は、`verified` フラグを User エンティティに追加し、未検証ユーザーのログインをブロックする設計を検討する
- マルチファミリー対応時に familyId 生成方式を再検討（現状: 自身の ID = familyId）
