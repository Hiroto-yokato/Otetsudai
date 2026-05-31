# 設計書

## アーキテクチャ概要

既存のレイヤードアーキテクチャに NotificationService を追加し、ChoreService・AllowanceService からメール送信を呼び出す構造にする。

```
ChoreService / AllowanceService
        │
        ▼ (通知トリガー)
NotificationService
        │
        ▼
JavaMailSender (Spring Boot Mail)
        │
        ▼
SMTP サーバー → 親のメールアドレス
```

## コンポーネント設計

### 1. NotificationService

**責務**:
- 親ユーザーをfamilyIdから検索する
- 親のメールアドレスが存在する場合のみメールを送信する
- メール送信失敗を内部でキャッチし、ログに記録して例外を伝播させない

**実装の要点**:
- `@Service` として登録
- `JavaMailSender` を DI で受け取る
- `UserRepository` を使って familyId から PARENT ロールのユーザーを検索
- 全メソッドで try-catch を使いメール失敗を飲み込む
- `SimpleMailMessage` でテキストメールを送信（HTML不要）
- 送信元アドレスは `application.properties` の `spring.mail.username` から取得

**メソッド**:
```java
void notifyParentChoreRegistered(User child, Chore chore)
void notifyParentBulkChoreRegistered(User child, List<Chore> chores)
void notifyParentAllowanceRequested(User child, AllowanceRequest request, List<Chore> chores)
```

### 2. pom.xml 変更

**追加依存**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

### 3. application.properties 変更

開発環境用のデフォルト設定（実際のSMTPなしで起動可能にする）:
```properties
spring.mail.host=localhost
spring.mail.port=25
spring.mail.username=noreply@otetsudai.local
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false
```

本番用（application-prod.properties）:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### 4. ChoreService 変更

`createChore()` と `createChoresBulk()` の末尾で NotificationService を呼び出す。

```java
// createChore の末尾
Chore saved = choreRepository.save(chore);
notificationService.notifyParentChoreRegistered(user, saved);
return saved;

// createChoresBulk の末尾
if (saved.isEmpty()) throw new IllegalStateException(...);
notificationService.notifyParentBulkChoreRegistered(user, saved);
return saved;
```

### 5. AllowanceService 変更

`createRequest()` の末尾で NotificationService を呼び出す。

```java
// createRequest の末尾
notificationService.notifyParentAllowanceRequested(user, savedRequest, chores);
return savedRequest;
```

## データフロー

### お手伝い登録通知
```
1. ChoreService.createChore() → choreRepository.save()
2. → NotificationService.notifyParentChoreRegistered(user, chore)
3. → userRepository.findByRoleAndFamilyId(PARENT, user.getFamilyId())
4. 親のemailがnullなら return (通知スキップ)
5. → JavaMailSender.send(message)
6. 失敗した場合 catch して log.warn() のみ
```

### お小遣い申請通知
```
1. AllowanceService.createRequest() → allowanceRequestRepository.save()
2. → NotificationService.notifyParentAllowanceRequested(user, request, chores)
3. → userRepository.findByRoleAndFamilyId(PARENT, user.getFamilyId())
4. 親のemailがnullなら return (通知スキップ)
5. → JavaMailSender.send(message)
6. 失敗した場合 catch して log.warn() のみ
```

## エラーハンドリング戦略

### メール送信失敗の処理

NotificationService の全メソッドは以下のパターンで実装:

```java
try {
    // メール送信処理
} catch (Exception e) {
    log.warn("メール送信失敗: {}", e.getMessage());
    // 例外を再スローしない
}
```

これにより、SMTP未設定の開発環境・SMTPサーバー障害時でもアプリの主要フローが継続する。

### familyId が null の場合

子どもに familyId が設定されていない場合（未紐付け）は親を検索しないで早期リターン。

## テスト戦略

### ユニットテスト (NotificationServiceTest)
- 親のメールあり → `JavaMailSender.send()` が1回呼ばれること
- 親のメールなし → `JavaMailSender.send()` が呼ばれないこと
- familyId が null → `JavaMailSender.send()` が呼ばれないこと
- JavaMailSender が例外を投げても → 例外が伝播しないこと（メソッドが正常終了）

### 既存テストの変更
- `ChoreServiceTest` と `AllowanceServiceTest` の既存テストで、NotificationService を mock に追加する必要がある

## 依存ライブラリ

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

## ディレクトリ構造

```
src/main/java/design/hitsuji/otetsudai/
└── service/
    ├── NotificationService.java   (新規)
    ├── ChoreService.java          (変更: NotificationService 注入・呼び出し追加)
    └── AllowanceService.java      (変更: NotificationService 注入・呼び出し追加)

src/main/resources/
├── application.properties         (変更: mail設定追加)
└── application-prod.properties    (変更: 本番mail設定追加)

src/test/java/design/hitsuji/otetsudai/service/
├── NotificationServiceTest.java   (新規)
├── ChoreServiceTest.java          (変更: NotificationService mock追加)
└── AllowanceServiceTest.java      (変更: NotificationService mock追加)
```

## 実装の順序

1. pom.xml に spring-boot-starter-mail 追加
2. application.properties にメール設定追加（開発用ダミー）
3. application-prod.properties に本番メール設定追加
4. NotificationService を実装
5. ChoreService に NotificationService を注入して呼び出し追加
6. AllowanceService に NotificationService を注入して呼び出し追加
7. NotificationServiceTest を作成
8. ChoreServiceTest を修正（NotificationService mock追加）
9. AllowanceServiceTest を修正（NotificationService mock追加）
10. mvn compile / mvn test で確認

## セキュリティ考慮事項

- SMTP 認証情報は環境変数で渡し、properties ファイルにハードコードしない
- メール本文に機密情報（パスワード等）を含めない
- メールアドレスはDBから取得したもののみ使用する

## パフォーマンス考慮事項

- メール送信は同期処理（MVP段階）。SMTP接続に時間がかかる場合は将来的に @Async 化を検討
- 家族単位の小規模利用では同期でも許容範囲内

## 将来の拡張性

- `@Async` + `@EnableAsync` でメール送信を非同期化
- `@TransactionalEventListener(phase = AFTER_COMMIT)` でトランザクションコミット後に送信
- HTML メールテンプレート（Thymeleafを使用）への切り替え
- 親が承認/却下した際の子どもへの通知追加
