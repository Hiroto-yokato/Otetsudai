# タスクリスト

## 🚨 タスク完全完了の原則
**このファイルの全タスクが完了するまで作業を継続すること**

---

## フェーズ1: 依存関係・設定

- [x] `pom.xml` に `spring-boot-starter-mail` を追加する
- [x] `application.properties` に開発用メール設定を追加する（localhost:25、認証なし）
- [x] `application-prod.properties` に本番用メール設定を追加する（環境変数参照）

## フェーズ2: NotificationService 実装

- [x] `NotificationService.java` を新規作成する
  - [x] `JavaMailSender` と `UserRepository` を DI
  - [x] `notifyParentChoreRegistered(User child, Chore chore)` を実装
    - [x] familyId が null なら早期リターン
    - [x] PARENT ロールの親ユーザーを検索
    - [x] 親の email が null なら早期リターン
    - [x] SimpleMailMessage でテキストメールを送信
    - [x] try-catch で例外を飲み込み log.warn() のみ
  - [x] `notifyParentBulkChoreRegistered(User child, List<Chore> chores)` を実装
    - [x] 件数・内容一覧をメール本文に含める
    - [x] 同様の早期リターン・例外ハンドリング
  - [x] `notifyParentAllowanceRequested(User child, AllowanceRequest request, List<Chore> chores)` を実装
    - [x] 申請金額・件数をメール本文に含める
    - [x] 同様の早期リターン・例外ハンドリング

## フェーズ3: 既存サービスへの組み込み

- [x] `ChoreService.java` に `NotificationService` を注入する
  - [x] コンストラクタに `NotificationService` を追加
  - [x] `createChore()` の末尾で `notifyParentChoreRegistered()` を呼び出す
  - [x] `createChoresBulk()` の末尾で `notifyParentBulkChoreRegistered()` を呼び出す
- [x] `AllowanceService.java` に `NotificationService` を注入する
  - [x] コンストラクタに `NotificationService` を追加
  - [x] `createRequest()` の末尾で `notifyParentAllowanceRequested()` を呼び出す

## フェーズ4: テスト

- [x] `NotificationServiceTest.java` を新規作成する
  - [x] `notifyParentChoreRegistered_withParentEmail_sendsEmail` テスト
  - [x] `notifyParentChoreRegistered_noParentEmail_skips` テスト
  - [x] `notifyParentChoreRegistered_nullFamilyId_skips` テスト
  - [x] `notifyParentChoreRegistered_mailException_doesNotPropagate` テスト
  - [x] `notifyParentBulkChoreRegistered_withParentEmail_sendsEmail` テスト
  - [x] `notifyParentAllowanceRequested_withParentEmail_sendsEmail` テスト
- [x] `ChoreServiceTest.java` を修正する
  - [x] `@Mock NotificationService notificationService` を追加
  - [x] `ChoreService` コンストラクタに `notificationService` を渡す（@InjectMocksが自動注入）
- [x] `AllowanceServiceTest.java` を修正する（存在する場合）
  - [x] `@Mock NotificationService notificationService` を追加（AllowanceServiceTest + AllowanceServiceApprovalTest 両方）
  - [x] `AllowanceService` コンストラクタに `notificationService` を渡す（@InjectMocksが自動注入）

## フェーズ5: ビルド確認

- [x] `mvn compile` が通ることを確認する
- [x] `mvn test` が通ることを確認する（全68テストパス）

---

## 実装後の振り返り

### 実装完了日
2026-05-30

### 計画と実績の差分

**バリデーターの指摘で追加した作業**:
- `ChoreServiceTest` と `AllowanceServiceTest` の成功系テストに `verify(notificationService)` を追加（通知呼び出しの回帰検出）
- `NotificationServiceTest` に `notifyParentBulkChoreRegistered` / `notifyParentAllowanceRequested` のメール未設定・例外ケースを追加（4件追加、計10テストに）
- pom.xml の maven-surefire-plugin に `-Dnet.bytebuddy.experimental=true` を追加（Java 25 + Byte Buddy の具象クラスmock制限への対応）

**最終テスト数**: 全72テストパス（追加: NotificationServiceTest 10件、既存+verify検証強化）

### 学んだこと

**技術的な学び**:
- `catch (MailException | RuntimeException e)` は `MailException` が `RuntimeException` のサブクラスのため、multi-catchに記述するとコンパイルエラーになる。`catch (Exception e)` で統一するのが安全
- Java 25 環境では Byte Buddy (1.15.11) が具象クラスの inline mock に失敗する（最大 Java 24 サポート）。surefire に `-Dnet.bytebuddy.experimental=true` を渡すことで回避可能。インターフェース (`JpaRepository` など) は問題なし
- `spring-boot-starter-mail` の開発用デフォルト設定として `localhost:25, auth=false` を指定しておくと、SMTP未設定環境でもアプリが起動でき、メール失敗を graceful に処理できる
- `application-prod.properties` は認証情報を `${MAIL_USERNAME}` / `${MAIL_PASSWORD}` の環境変数参照にすれば、ファイル自体をリポジトリに含めても安全

**プロセス上の改善点**:
- 通知系サービスを追加するとき、既存サービステストの mock 追加だけでなく、成功系テストでの verify 追加まで計画段階に含めるべき

### 次回への改善提案
- メール送信を `@Async` + `@EnableAsync` で非同期化すると、SMTP遅延がHTTPレスポンスタイムに影響しなくなる
- `@TransactionalEventListener(phase = AFTER_COMMIT)` を使うと、トランザクションロールバック時にメールが誤送信されるケースを防げる
- `AllowanceService.notifyParentAllowanceRequested` で全親ユーザー（複数）に送信する拡張を検討（現在は最初の1人のみ）
