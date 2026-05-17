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

- [x] `ChoreStatus.java`（enum）を作成する
- [x] `Chore.java`（JPA エンティティ）を作成する
- [x] `ChoreForm.java`（DTO）を作成する
- [x] `ChoreRepository.java`（Spring Data JPA インターフェース）を作成する

## フェーズ2: サービス層

- [x] `ChoreService.java` を作成する
  - [x] `listChores(String loginUserId)` — 日付降順で全件取得
  - [x] `createChore(String loginUserId, LocalDate choreDate, String content)` — UNPAID で保存
  - [x] `deleteChore(Long choreId, String loginUserId)` — UNPAID かつ所有者チェック後削除

## フェーズ3: コントローラー層

- [x] `ChoreController.java` を作成する
  - [x] `GET /chores` — 一覧取得
  - [x] `GET /chores/new` — 登録フォーム表示
  - [x] `POST /chores` — 登録処理（バリデーション付き）
  - [x] `POST /chores/{id}/delete` — 削除処理

## フェーズ4: ビュー層

- [x] `templates/chore/list.html` を作成する（お手伝い一覧）
  - [x] 共通レイアウト（`layout :: head`, `layout :: nav`）を使用する
  - [x] 登録完了アニメーションを実装する（`?registered=true` 時に表示）
  - [x] お手伝い一覧テーブルを実装する（日付・内容・ステータスバッジ）
  - [x] UNPAID のみ削除ボタンを表示する
- [x] `templates/chore/new.html` を作成する（登録フォーム）
  - [x] 共通レイアウトを使用する
  - [x] 日付 input（type="date"）と内容 textarea を実装する
  - [x] バリデーションエラーメッセージを表示する
- [x] `static/css/main.css` にお手伝い関連スタイルを追加する
  - [x] お手伝い一覧テーブルのスタイル
  - [x] ステータスバッジ（UNPAID/PENDING/APPROVED/REJECTED）のスタイル
  - [x] 登録完了アニメーション（fadeIn + 3秒で消滅）のスタイル

## フェーズ5: テスト

- [x] `ChoreServiceTest.java` を作成する
  - [x] `createChore` — 正常: Chore が UNPAID で保存されること
  - [x] `deleteChore` — 正常: UNPAID の Chore が削除されること
  - [x] `deleteChore` — 異常: PENDING の Chore は `IllegalStateException` が投げられること
  - [x] `deleteChore` — 異常: 他ユーザーの Chore は `IllegalStateException` が投げられること

## フェーズ6: ビルド確認

- [x] `mvn compile` が通ることを確認する
- [x] `mvn test` が通ることを確認する
- [x] `mvn spring-boot:run` で起動し、ブラウザで `/chores` が表示されることを確認する

---

## 実装後の振り返り

### 実装完了日
2026-05-16

### 計画と実績の差分

**計画と異なった点**:
- design.md のメソッド名 `findByUserIdOrderByChoreDate​Desc` に U+200B（ゼロ幅スペース）が混入しており、ChoreRepository と ChoreService にそのまま含まれた。アプリは動作したが（Spring Data JPA が U+200B を無視した可能性）、コード品質上の問題として修正した。

**新たに必要になったタスク**:
- `ChoreServiceTest` に `listChores` テスト追加（validator 指摘）
- `ChoreServiceTest` の `deleteChore` を PENDING/APPROVED/REJECTED の `@ParameterizedTest` に変更（validator 指摘）
- `ChoreRepository` / `ChoreService` の U+200B 除去（validator 指摘）

### 学んだこと

**技術的な学び**:
- ドキュメント（design.md）から Java ファイルにコピーする際、不可視文字（U+200B）が混入する可能性がある。Write ツール経由でコピーされたテキストも安全とは限らないため、特に Spring Data JPA のメソッド名はバイト列で確認すべき
- `@ParameterizedTest` + `@EnumSource` を使うと、複数の enum 値に対して同じ検証を簡潔に書ける
- PowerShell の `-c` / `-b` フラグなしで curl からセッションを使い回すと、CSRF トークンとセッションが不一致になりログインが失敗する

**プロセス上の改善点**:
- design.md のメソッド名は「正確な Java 識別子」であることを確認してから Java ファイルに転記する
- Bash/curl でセッション付きのログインテストをする際はクッキーファイルを GET と POST で共有すること

### 次回への改善提案
- Spring Data JPA のメソッド名に非 ASCII 文字が含まれていないか、compile フェーズ後に Grep で確認するステップをビルド確認に追加する
- テスト追加は「validator 指摘後に追加」ではなく計画フェーズで予め含める（listChores テストは最初から tasklist にあるべきだった）
