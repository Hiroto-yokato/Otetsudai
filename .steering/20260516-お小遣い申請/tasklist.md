# タスクリスト

## 🚨 タスク完全完了の原則

**このファイルの全タスクが完了するまで作業を継続すること**

### 必須ルール
- **全てのタスクを`[x]`にすること**
- 「時間の都合により別タスクとして実施予定」は禁止
- 「実装が複雑すぎるため後回し」は禁止
- 未完了タスク（`[ ]`）を残したまま作業を終了しない

---

## フェーズ1: ドメイン層

- [x] `RequestStatus.java`（enum）を作成する
- [x] `AllowanceRequest.java`（JPA エンティティ）を作成する
- [x] `AllowanceRequestRepository.java` を作成する
- [x] `ChoreRepository.java` に `findByIdInAndUserId` を追加する

## フェーズ2: サービス層

- [x] `ChoreService.java` に `listUnpaidChores` を追加する
- [x] `AllowanceService.java` を作成する
  - [x] `calculateAmount(List<Chore> chores)` — 日付グループ別金額計算
  - [x] `previewAmount(String loginUserId, List<Long> choreIds)` — AJAX 用金額計算
  - [x] `createRequest(String loginUserId, List<Long> choreIds)` — 申請確定・ステータス更新

## フェーズ3: コントローラー層

- [x] `RequestController.java` を作成する
  - [x] `GET /requests/new` — 未申請お手伝い一覧を渡して申請フォーム表示
  - [x] `POST /requests/preview` — AJAX 金額計算、JSON 返却
  - [x] `POST /requests` — 申請確定、エラーハンドリング

## フェーズ4: ビュー層

- [x] `templates/chore/list.html` を更新する
  - [x] `?applied=true` のとき申請完了バナーを表示する
  - [x] 「お小遣いを申請する」ボタンを追加する（UNPAID が存在する場合）
- [x] `templates/request/new.html` を作成する
  - [x] 共通レイアウトを使用する
  - [x] UNPAID お手伝い一覧をチェックボックスで表示する
  - [x] AJAX 金額プレビューを実装する（fetch POST /requests/preview）
  - [x] 申請ボタン（POST /requests）を実装する
- [x] `static/css/main.css` に申請フォーム関連スタイルを追加する

## フェーズ5: テスト

- [x] `AllowanceServiceTest.java` を作成する
  - [x] `calculateAmount` — 1件: ¥20
  - [x] `calculateAmount` — 2件（同日）: ¥50
  - [x] `calculateAmount` — 3件（同日）: ¥100
  - [x] `calculateAmount` — 複数日合算（1日1件 + 1日2件 = ¥70）
  - [x] `createRequest` — 正常: AllowanceRequest が PENDING で保存されること
  - [x] `createRequest` — 正常: 対象 Chore が PENDING になりリクエスト ID がセットされること
  - [x] `createRequest` — 異常: 空リストで `IllegalStateException`
  - [x] `createRequest` — 異常: 他ユーザーの Chore が含まれていると `IllegalStateException`

## フェーズ6: ビルド確認

- [x] `mvn compile` が通ることを確認する
- [x] `mvn test` が通ることを確認する
- [x] `mvn spring-boot:run` で起動し、`/requests/new` が表示されることを確認する

---

## 実装後の振り返り

### 実装完了日
2026-05-17

### 計画と実績の差分

**計画と異なった点**:
- `AllowanceService.calculateAmount` のシグネチャをスペックの `(List<Long> choreIds)` から `(List<Chore> chores)` に変更。DB アクセスなしで単体テストが書きやすくなり設計が改善された
- `ChoreRepository` のスペックに定義された `findByIdInAndStatus` は `findByIdInAndUserId` に変更。ユーザー所有確認と取得を1クエリにまとめた
- バリデーター指摘を受け追加した修正: マジックナンバー定数化、`catch (Exception)` → `catch (IllegalStateException)` 絞り込み、テスト3件追加（計22件）

**新たに必要になったタスク**:
- バリデーター指摘対応: `AllowanceService` マジックナンバーを定数に抽出
- バリデーター指摘対応: `RequestController.preview` の例外補足を限定
- バリデーター指摘対応: `calculateAmount` 空リストテスト、PENDING Chore で createRequest テスト、`listUnpaidChores` テスト追加

### 学んだこと

**技術的な学び**:
- AJAX POST で Spring Security の CSRF を通すには、Thymeleaf が `th:action` フォームに自動挿入する `input[name="_csrf"]` を JavaScript で読み取って `URLSearchParams` に追加する方法が有効
- Spring Security は `application/x-www-form-urlencoded` 形式の POST であればリクエストボディから CSRF トークンを読める
- `findByIdInAndUserId` の size 比較で「他ユーザーID」「存在しないID」の両方を一度に弾ける設計はシンプルで効果的

**プロセス上の改善点**:
- `ChoreController` の `list()` に `hasUnpaid` を追加したように、テンプレートで SpEL コレクション選択を使うより Controller でフラグを計算する方が保守性が高い

### 次回への改善提案
- 承認・却下フェーズ (ApprovalController) で `AllowanceRequest` の status を APPROVED/REJECTED に変更し、Chore を PENDING → APPROVED/UNPAID に戻す実装が次フェーズ
- CSRF トークン取得を `<meta>` タグ経由に統一するとフォーム DOM 構造への依存をなくせる（提案レベル）
