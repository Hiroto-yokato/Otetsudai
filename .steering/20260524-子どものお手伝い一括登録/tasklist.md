# タスクリスト

## 🚨 タスク完全完了の原則
**このファイルの全タスクが完了するまで作業を継続すること**

---

## フェーズ1: DTO 層

- [x] `BulkChoreForm.java` を新規作成する
  - [x] 内部クラス `ChoreEntryForm`（`choreDate`, `content`）
  - [x] `entries` フィールド（`List<ChoreEntryForm>`）と getter/setter

## フェーズ2: サービス層

- [x] `ChoreService.java` に `createChoresBulk(String loginUserId, BulkChoreForm form)` を追加する
  - [x] content が空の行はスキップ
  - [x] choreDate が null の行は当日日付をデフォルト
  - [x] 有効行が 0 件 → IllegalStateException
  - [x] 有効行を save して返す

## フェーズ3: コントローラー層

- [x] `ChoreController.java` に `GET /chores/bulk`（bulkForm）を追加する
  - [x] 5行・今日の日付をデフォルト入力した BulkChoreForm をモデルに追加
  - [x] choreTemplates もモデルに追加
- [x] `ChoreController.java` に `POST /chores/bulk`（createBulk）を追加する
  - [x] IllegalStateException → errorMessage → chore/bulk に戻る
  - [x] 成功 → redirect:/chores?registered=true

## フェーズ4: ビュー層

- [x] `chore/bulk.html` を新規作成する
  - [x] テーブル形式（日付列・内容列）で entries を描画
  - [x] datalist (#choreTemplates) を使用
  - [x] 「行を追加」ボタン + JavaScript で新行を動的追加
  - [x] 「まとめて登録する」送信ボタン / 「キャンセル」リンク
- [x] `chore/list.html` に「まとめて登録する」リンクを追加する

## フェーズ5: テスト

- [x] `ChoreServiceTest.java` に `createChoresBulk_success` テストを追加する（複数行が保存されること）
- [x] `ChoreServiceTest.java` に `createChoresBulk_skipsEmptyEntries` テストを追加する
- [x] `ChoreServiceTest.java` に `createChoresBulk_allEmpty_throwsIllegalStateException` テストを追加する
- [x] `ChoreServiceTest.java` に `createChoresBulk_nullDate_usesToday` テストを追加する

## フェーズ6: ビルド確認

- [x] `mvn compile` が通ることを確認する
- [x] `mvn test` が通ることを確認する（最終: 全62テストパス）

---

## 実装後の振り返り

### 実装完了日
2026-05-24

### 計画と実績の差分

**バリデーターの指摘で追加した作業**:
- `createChoresBulk` に未来日付ガードを追加（functional-design.md の「未来日不可」制約への準拠）
- `createChoresBulk` に content 100文字超チェックを追加（DB 制約エラーのユーザーフレンドリー化）
- テスト3件追加（`futureDate`, `contentTooLong`, `contentIsTrimmed`）

### 学んだこと

**技術的な学び**:
- Thymeleaf の `th:name="'entries[' + ${iter.index} + '].choreDate'"` でインデックスベースのリストバインディングができる。`th:field` は動的追加行に使えないため、JavaScript 追加行には直接 name 属性を書く
- `/*[[${...}]]*/` 形式の Thymeleaf インライン JavaScript は JS エスケープが自動適用されるため安全
- Bean Validation の `@Valid` を List 要素に適用するのは複雑（空行スキップとの組み合わせがやりにくい）ので、一括入力フォームはサービス層でまとめてバリデーションする方がシンプル

**プロセス上の改善点**:
- 単一登録フォームに `@PastOrPresent` がある場合、一括登録でも同様の制約を設計段階から考慮すべき

### 次回への改善提案
- 行数の上限（例: 最大20行）を設けてもよい
- 将来的に登録後のアニメーション（⭐バナー）を件数分表示するなど UX 改善が可能
