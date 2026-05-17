# 設計書

## アーキテクチャ概要

既存のレイヤードアーキテクチャを踏襲。`UserService` に `registerParent` メソッドを追加し、`AuthController` に登録エンドポイントを追加する。

```
Browser
  ↓ GET /register        ← permitAll（ログイン不要）
AuthController → "auth/register" テンプレート

Browser
  ↓ POST /register
AuthController → UserService.registerParent(form)
  ↓                ├─ userId 重複確認
                   ├─ パスワード一致確認
                   ├─ email 重複確認
                   ├─ BCrypt ハッシュ化
                   ├─ User(PARENT) 保存 → id 取得
                   └─ familyId = id → 再保存
  ↓
redirect:/login?registered=true
```

## コンポーネント設計

### 1. ParentRegistrationForm.java（DTO 新規）

`UserRegistrationForm` と共通フィールドだが、`email` が `@NotBlank` 必須:

```java
public class ParentRegistrationForm {
    @NotBlank @Size(min=3, max=20)
    @Pattern(regexp="^[a-zA-Z0-9_]+$")
    private String userId;

    @NotBlank @Size(min=8, max=100)
    private String password;

    @NotBlank
    private String passwordConfirm;

    @NotBlank(message="メールアドレスを入力してください")
    @Email(message="正しいメールアドレスの形式で入力してください")
    @Size(max=200)
    private String email;  // 親は必須

    @Size(max=50)
    private String displayName; // 任意
}
```

### 2. UserService.java に追記

**registerParent(ParentRegistrationForm form)**:
1. `existsByUserId(form.getUserId())` → true なら `IllegalStateException`
2. パスワードと確認が不一致 → `IllegalStateException`
3. `existsByEmail(form.getEmail())` → true なら `IllegalStateException`
4. User 作成: role=PARENT, BCrypt ハッシュ
5. `userRepository.save(user)` → id 取得
6. `user.setFamilyId(user.getId())` → `userRepository.save(user)` で familyId を確定

### 3. AuthController.java 更新

```java
GET /register → model: form=new ParentRegistrationForm() → "auth/register"

POST /register
  → @Valid ParentRegistrationForm
  → バリデーションエラー → "auth/register"
  → registerParent() → redirect:/login?registered=true
  → IllegalStateException → model.addAttribute("errorMessage") → "auth/register"
```

### 4. SecurityConfig.java 更新

```java
.requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
```

### 5. templates/auth/register.html（新規）

- ログインページと同様のカード型レイアウト
- エラーメッセージ表示
- フォームフィールド: userId, password, passwordConfirm, email（必須）, displayName（任意）
- 「登録する」ボタン
- 「ログインに戻る」リンク

### 6. templates/auth/login.html 更新

- カード下部に「アカウントをお持ちでない方はこちら → /register」リンクを追加

### 7. main.css 追記

- register ページのスタイル（ログインページと同様のデザインを流用）

## エラーハンドリング

| エラー | 処理 |
|--------|------|
| Bean Validation エラー | フォームに戻り `th:errors` で表示 |
| ユーザーID 重複 | `IllegalStateException` → `errorMessage` → フォームに戻る |
| Email 重複 | 同上 |
| パスワード不一致 | 同上 |

## familyId の設計

親ユーザー登録時、同一家族であることを示す familyId を自動生成する。最もシンプルな実装として「保存後の自身の DB ID を familyId として使用」する方式を採用する。これにより:
- 外部の採番テーブルが不要
- 各親アカウントは必ず一意の familyId を持つ
- 子どもはこの familyId を継承する（既実装）

## テスト戦略

### UserServiceTest 追加（JUnit 5 + Mockito）

- `registerParent_success`: PARENT ユーザーが保存され、familyId = id になること
- `registerParent_duplicateUserId`: IllegalStateException が発生すること
- `registerParent_duplicateEmail`: IllegalStateException が発生すること
- `registerParent_passwordMismatch`: IllegalStateException が発生すること
