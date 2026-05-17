# 設計書

## アーキテクチャ概要

既存のレイヤードアーキテクチャを踏襲。新規 UserService と UserController を追加し、User エンティティに displayName を追加する。

```
Browser
  ↓ GET /users
UserController  ← PARENT ロールのみアクセス可
  ↓ listChildren(loginUserId)
UserService → UserRepository.findByRoleAndFamilyId()

Browser
  ↓ GET /users/new
UserController  → "users/new" テンプレート

Browser
  ↓ POST /users
UserController → UserService.registerChild(loginUserId, form)
  ↓                ├─ userId 重複確認
                   ├─ email 重複確認（設定時）
                   ├─ パスワード一致確認
                   ├─ BCrypt ハッシュ化
                   └─ User(CHILD) 保存
  ↓
redirect:/users?registered=true
```

## コンポーネント設計

### 1. User.java エンティティ変更

`displayName` フィールドを追加:

```java
@Column(name = "display_name", length = 50)
private String displayName;
// getter/setter 追加
```

### 2. UserRepository.java 追記

```java
List<User> findByRoleAndFamilyId(Role role, Long familyId);
boolean existsByUserId(String userId);
boolean existsByEmail(String email);
```

`existsBy` メソッドは重複チェックを 1 クエリで行うために使用。

### 3. UserRegistrationForm.java（DTO 新規）

```java
public class UserRegistrationForm {
    @NotBlank @Size(min=3, max=20)
    @Pattern(regexp="[a-zA-Z0-9_]+", message="ユーザーIDは英数字・アンダースコアのみ使用できます")
    private String userId;

    @NotBlank @Size(min=8, max=100, message="パスワードは8文字以上で入力してください")
    private String password;

    @NotBlank(message="パスワード確認を入力してください")
    private String passwordConfirm;

    @Email(message="正しいメールアドレスの形式で入力してください")
    @Size(max=200)
    private String email; // optional (empty string → null)

    @Size(max=50, message="表示名は50文字以内で入力してください")
    private String displayName; // optional
}
```

パスワード一致確認はサービス層で実施（Bean Validation のクロスフィールドバリデーターは複雑なため）。

### 4. UserService.java（新規）

**registerChild(String loginUserId, UserRegistrationForm form)**:
1. 親ユーザーを `findUserByLoginId(loginUserId)` で取得
2. `existsByUserId(form.getUserId())` → true なら `IllegalStateException("このユーザーIDは既に使用されています")`
3. パスワードと確認が不一致 → `IllegalStateException("パスワードが一致しません")`
4. `email` が空文字でなく `existsByEmail(email)` → true なら `IllegalStateException("このメールアドレスは既に使用されています")`
5. User 作成: role=CHILD, familyId=parent.familyId, userId, email(null化), displayName, passwordHash=BCrypt
6. `userRepository.save(user)` → 保存

**listChildren(String loginUserId)**:
- 親ユーザーを取得
- `findByRoleAndFamilyId(Role.CHILD, parent.getFamilyId())` を返す

### 5. UserController.java（新規）

```java
@Controller
@RequestMapping("/users")
public class UserController {

    GET /users
        → model: children=listChildren()
        → "users/list"

    GET /users/new
        → model: form=new UserRegistrationForm()
        → "users/new"

    POST /users
        → @Valid UserRegistrationForm
        → バリデーションエラー → "users/new"
        → registerChild() → redirect:/users?registered=true
        → IllegalStateException → model.addAttribute("errorMessage") → "users/new"
}
```

### 6. SecurityConfig.java 更新

```java
.requestMatchers("/approvals/**", "/history", "/users/**").hasRole("PARENT")
```

### 7. templates/users/list.html（新規）

- 共通レイアウト使用
- 子どもアカウント一覧テーブル: userId、表示名、登録日
- 「子どもを追加する」ボタン → /users/new
- `?registered=true` → 登録完了バナー
- 空状態メッセージ

### 8. templates/users/new.html（新規）

- 共通レイアウト使用
- エラーメッセージ表示（重複等のサービス層エラー）
- フォームフィールド:
  - userId (text, 必須)
  - password (password, 必須)
  - passwordConfirm (password, 必須)
  - email (email, 任意)
  - displayName (text, 任意)
- バリデーションエラー表示（フィールド別）
- 「登録する」/「キャンセル」ボタン

### 9. main.css 追記

- users/ 画面のスタイル（既存フォームスタイルを流用できるため追加は最小限）

## エラーハンドリング

| エラー | 処理 |
|--------|------|
| Bean Validation エラー | フォームに戻り `th:errors` で表示 |
| ユーザーID 重複 | `IllegalStateException` → `model.addAttribute("errorMessage")` → フォームに戻る |
| Email 重複 | 同上 |
| パスワード不一致 | 同上 |

## テスト戦略

### UserServiceTest（JUnit 5 + Mockito）
- `registerChild_success`: CHILD ユーザーが正しいフィールドで保存されること
- `registerChild_duplicateUserId`: IllegalStateException が発生すること
- `registerChild_duplicateEmail`: IllegalStateException が発生すること
- `registerChild_passwordMismatch`: IllegalStateException が発生すること
- `listChildren_returnsChildrenOfSameFamily`: CHILD かつ同一 familyId のみ返ること

## ディレクトリ構造

```
src/
├── main/java/design/hitsuji/otetsudai/
│   ├── entity/
│   │   └── User.java               ← displayName 追加
│   ├── repository/
│   │   └── UserRepository.java     ← findByRoleAndFamilyId, existsBy 追記
│   ├── dto/
│   │   └── UserRegistrationForm.java ← 新規
│   ├── service/
│   │   └── UserService.java        ← 新規
│   └── controller/
│       └── UserController.java     ← 新規
├── main/resources/
│   ├── templates/users/
│   │   ├── list.html               ← 新規
│   │   └── new.html                ← 新規
│   └── static/css/
│       └── main.css                ← 最小追記
└── test/java/design/hitsuji/otetsudai/
    └── service/
        └── UserServiceTest.java    ← 新規
```

## 実装順序

1. User.java に displayName 追加
2. UserRepository.java に findByRoleAndFamilyId / existsBy 追記
3. UserRegistrationForm.java 新規作成
4. UserService.java 新規作成
5. UserController.java 新規作成
6. SecurityConfig.java 更新
7. templates/users/list.html 新規作成
8. templates/users/new.html 新規作成
9. main.css 最小追記
10. UserServiceTest.java 新規作成
