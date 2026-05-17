# 設計書

## アーキテクチャ概要

Spring Security のフォーム認証を使用したレイヤードアーキテクチャ。

```
Browser
  │  POST /login (userId or email + password)
  ▼
SecurityConfig（Spring Security フィルターチェーン）
  │  UserDetailsServiceImpl.loadUserByUsername()
  ▼
UserRepository（DB照合）
  │  PasswordEncoder（BCrypt 検証）
  ▼
認証成功 → ロール別リダイレクト（AuthSuccessHandler）
認証失敗 → /login?error
```

## コンポーネント設計

### 1. pom.xml

**責務**: Mavenプロジェクト定義と依存関係管理

**実装の要点**:
- `spring-boot-starter-web`（Spring MVC + Tomcat）
- `spring-boot-starter-security`（Spring Security）
- `spring-boot-starter-thymeleaf`（テンプレートエンジン）
- `thymeleaf-extras-springsecurity6`（Thymeleaf + Security 統合）
- `spring-boot-starter-data-jpa`（Spring Data JPA + Hibernate）
- `org.xerial:sqlite-jdbc`（SQLiteドライバ）
- `org.hibernate.orm:hibernate-community-dialects`（SQLite用Dialectサポート）
- `spring-boot-starter-validation`（Bean Validation）
- `spring-boot-starter-test`（JUnit5 + Mockito）

### 2. OtetsudaiApplication.java

**責務**: Spring Boot エントリーポイント

### 3. User エンティティ

**責務**: DB の `users` テーブルとのマッピング

**実装の要点**:
- `@Entity`, `@Table(name = "users")`
- フィールド: `id`(PK), `userId`(UK), `email`(UK nullable), `passwordHash`, `role`, `familyId`, `createdAt`, `updatedAt`
- `@Column(unique = true, nullable = false)` を `userId` に付与
- `@Column(unique = true)` を `email` に付与（nullable = true がデフォルト）

### 4. Role 列挙型

**責務**: PARENT / CHILD のロール定義

### 5. UserRepository

**責務**: User エンティティの CRUD と認証用クエリ

**実装の要点**:
```java
Optional<User> findByUserId(String userId);
Optional<User> findByEmail(String email);
Optional<User> findByUserIdOrEmail(String userId, String email);
```

### 6. UserDetailsServiceImpl

**責務**: Spring Security の `UserDetailsService` 実装。ログインIDでユーザーを検索し UserDetails を返す。

**実装の要点**:
- `loadUserByUsername(String loginId)` で `userId` または `email` でユーザー検索
- `UserRepository.findByUserIdOrEmail(loginId, loginId)` を呼び出す
- 見つからない場合 `UsernameNotFoundException` をスロー
- `User.role` を `ROLE_PARENT` / `ROLE_CHILD` に変換して `SimpleGrantedAuthority` に渡す

### 7. SecurityConfig

**責務**: Spring Security のフィルターチェーン・認証・認可の設定

**実装の要点**:
```java
@Configuration
@EnableWebSecurity
class SecurityConfig {
    // /login, /css/**, /js/** は permitAll
    // /approvals/**, /history は ROLE_PARENT
    // /chores/**, /requests/** は ROLE_CHILD
    // その他は認証必須
    // formLogin: loginPage("/login"), defaultSuccessUrl は AuthSuccessHandler で制御
    // logout: logoutSuccessUrl("/login")
    // BCryptPasswordEncoder を Bean として登録
}
```

### 8. AuthSuccessHandler

**責務**: ログイン成功後のロール別リダイレクト

**実装の要点**:
- `ROLE_PARENT` → `/approvals`
- `ROLE_CHILD` → `/chores`

### 9. AuthController

**責務**: `GET /login` でログインページを返す（POST は Spring Security が処理）

### 10. login.html（Thymeleaf）

**責務**: ログインフォームの表示

**実装の要点**:
- `action="/login"` の POST フォーム
- `name="username"` フィールド（ユーザーID or メール）
- `name="password"` フィールド
- CSRF トークンを Thymeleaf で自動付与（`th:action="@{/login}"`）
- `?error` パラメータがある場合にエラーメッセージを表示
- `?logout` パラメータがある場合にログアウトメッセージを表示

### 11. DataInitializer

**責務**: 開発用初期データ（親・子どもアカウント）の投入

**実装の要点**:
- `CommandLineRunner` を実装
- 起動時に users テーブルが空なら初期データを INSERT
- 親: userId=`parent`, password=`parent123`
- 子ども: userId=`child`, password=`child123`

### 12. application.properties

**設定内容**:
- `spring.datasource.url=jdbc:sqlite:./app-data/otetsudai.db`
- `spring.datasource.driver-class-name=org.sqlite.JDBC`
- `spring.jpa.hibernate.ddl-auto=update`
- `spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect`

### 13. UserDetailsServiceImplTest

**責務**: `loadUserByUsername` のユニットテスト

## データフロー

### ログイン成功フロー
```
1. ブラウザが POST /login {username=parent, password=parent123} を送信
2. Spring Security フィルターが UserDetailsServiceImpl.loadUserByUsername("parent") を呼び出す
3. UserRepository.findByUserIdOrEmail("parent", "parent") でユーザーを検索
4. BCryptPasswordEncoder でパスワードを検証
5. 認証成功 → AuthSuccessHandler が ROLE_PARENT なら /approvals にリダイレクト
```

### ログイン失敗フロー
```
1. ブラウザが POST /login {username=wrong, password=wrong} を送信
2. UsernameNotFoundException または BadCredentialsException が発生
3. /login?error にリダイレクト
4. login.html がエラーメッセージを表示
```

## エラーハンドリング戦略

- `UsernameNotFoundException`: `UserDetailsServiceImpl` でスローし、Spring Security が BadCredentials に変換して `/login?error` へリダイレクト
- 403 Forbidden: Spring Security デフォルトの 403 エラーページ（将来的にカスタムページを追加可）

## テスト戦略

### ユニットテスト
- `UserDetailsServiceImplTest`: `loadUserByUsername` を userId/email 両方でテスト、存在しないユーザーで `UsernameNotFoundException` が投げられることを確認

### 統合テスト
- ログイン成功/失敗フロー（`@SpringBootTest` + MockMvc）

## ディレクトリ構造

```
src/
├── main/
│   ├── java/design/hitsuji/otetsudai/
│   │   ├── OtetsudaiApplication.java
│   │   ├── config/
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   └── AuthController.java
│   │   ├── entity/
│   │   │   └── User.java
│   │   ├── enums/
│   │   │   └── Role.java
│   │   ├── repository/
│   │   │   └── UserRepository.java
│   │   ├── service/
│   │   │   ├── UserDetailsServiceImpl.java
│   │   │   └── AuthSuccessHandler.java
│   │   └── init/
│   │       └── DataInitializer.java
│   └── resources/
│       ├── templates/
│       │   ├── auth/
│       │   │   └── login.html
│       │   └── fragments/
│       │       └── layout.html
│       ├── static/
│       │   └── css/
│       │       └── main.css
│       └── application.properties
├── test/
│   └── java/design/hitsuji/otetsudai/
│       └── service/
│           └── UserDetailsServiceImplTest.java
├── app-data/           (gitignore対象)
└── pom.xml
```

## 実装の順序

1. pom.xml 作成（プロジェクト基盤）
2. application.properties 作成
3. OtetsudaiApplication.java 作成
4. Role 列挙型作成
5. User エンティティ作成
6. UserRepository 作成
7. UserDetailsServiceImpl 作成
8. AuthSuccessHandler 作成
9. SecurityConfig 作成
10. AuthController 作成
11. login.html 作成（Thymeleaf）
12. layout.html（共通レイアウト）作成
13. main.css 作成（最小限）
14. DataInitializer 作成
15. UserDetailsServiceImplTest 作成
16. app-data/ ディレクトリ、.gitignore 作成

## セキュリティ考慮事項

- BCrypt strength=12 でパスワードをハッシュ化
- CSRF: Spring Security デフォルト有効、Thymeleaf の `th:action` で自動付与
- セッション固定攻撃対策: Spring Security デフォルトで sessionFixation().changeSessionId() が有効
- スタックトレースをレスポンスに含めない（Spring Boot デフォルト設定）

## 将来の拡張性

- `familyId` をUserエンティティに持たせることでマルチファミリー対応の土台を用意
- DataInitializer を削除して管理者UIからユーザー作成する仕組みに置き換え可能
