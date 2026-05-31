# タスクリスト

## 🚨 タスク完全完了の原則
**このファイルの全タスクが完了するまで作業を継続すること**

---

## フェーズ1: リポジトリ層

- [x] `ChoreRepository.java` に `findByUserIdInOrderByChoreDateDesc(List<Long> userIds)` を追加する

## フェーズ2: サービス層

- [x] `ChoreService.java` に `updateChore(Long choreId, String loginUserId, ChoreForm form)` を追加する
  - [x] UNPAID 以外は IllegalStateException
  - [x] 他人のお手伝いは IllegalStateException
  - [x] 正常時は保存して返す
- [x] `ChoreService.java` に `listFamilyChores(String parentUserId)` を追加する
  - [x] 親の familyId → 子ども Users → 全 chores を返す
  - [x] familyId が null の場合は空リストを返す
- [x] `ChoreService.java` に `buildUserMap(List<Long> userIds)` を追加する（子ども名表示用）

## フェーズ3: コントローラー層

- [x] `ChoreController.java` に定数 `CHORE_TEMPLATES`（よく使うお手伝い内容リスト）を追加する
- [x] `ChoreController.java` の `newForm` に `choreTemplates` をモデルに追加する
- [x] `ChoreController.java` に `GET /chores/{id}/edit` を追加する（editForm）
- [x] `ChoreController.java` に `POST /chores/{id}/edit` を追加する（update）
- [x] `ChoreController.java` に `GET /chores/family` を追加する（familyList, PARENT 向け）

## フェーズ4: セキュリティ設定

- [x] `SecurityConfig.java` に `/chores/family` を PARENT 許可ルールとして追加する（`/chores/**` より前に宣言）

## フェーズ5: ビュー層

- [x] `chore/new.html` の textarea を `<input type="text" list="choreTemplates">` + `<datalist>` に変更する
- [x] `chore/edit.html` を新規作成する（new.html と同構造、action を `POST /chores/{id}/edit` に）
- [x] `chore/list.html` に UNPAID 行の「編集」ボタンを追加する
- [x] `chore/family.html` を新規作成する（子ども名・日付・内容・状態のテーブル）

## フェーズ6: テスト

- [x] `ChoreServiceTest.java` に `updateChore_success` テストを追加する
- [x] `ChoreServiceTest.java` に `updateChore_notUnpaid_throwsIllegalStateException` テストを追加する
- [x] `ChoreServiceTest.java` に `updateChore_otherUser_throwsIllegalStateException` テストを追加する
- [x] `ChoreServiceTest.java` に `listFamilyChores_returnsAllChildrenChores` テストを追加する
- [x] `ChoreServiceTest.java` に `listFamilyChores_noFamilyId_returnsEmpty` テストを追加する

## フェーズ7: ビルド確認

- [x] `mvn compile` が通ることを確認する
- [x] `mvn test` が通ることを確認する（全55テストパス）

---

## 実装後の振り返り

### 実装完了日
2026-05-23

### 計画と実績の差分

**新たに必要になった作業**:
- `getChoreForEdit` メソッドの追加（コントローラから編集権限チェックを分離するために必要）
- `findEditableChore` プライベートメソッドの抽出（`getChoreForEdit` / `updateChore` / `deleteChore` のコード重複解消）
- セキュリティ修正（バリデーターが指摘）:
  - `listFamilyChores` に PARENT ロール確認を追加
  - 認可エラーを `IllegalStateException` → `AccessDeniedException` に変更

### 学んだこと

**技術的な学び**:
- Spring Security の `authorizeHttpRequests` は先着マッチングのため、`/chores/family` を `/chores/**` より前に置く必要がある。ただしこれだけでは多層防御にならないため、サービス層でもロールを確認するのが堅牢
- 認可エラー（他人のリソースへのアクセス）と業務ルール違反（UNPAID 以外は操作不可）は別の例外クラスで表現すべき。前者は `AccessDeniedException`、後者は `IllegalStateException`
- `findEditableChore` のような共通バリデーションの抽出で、メッセージが統一されてテストも簡潔になる（ただし既存テストのメッセージチェックも合わせて更新が必要）

**プロセス上の改善点**:
- 実装バリデーターが `AccessDeniedException` への変更とロールチェック追加を指摘してくれた。セキュリティ関係は実装時から意識して設計すべき

### 次回への改善提案
- `CHORE_TEMPLATES` 定数を `application.properties` に外出しすると、再ビルドなしに候補を追加できる
- functional-design.md の画面一覧に `/chores/{id}/edit`（CHILD）と `/chores/family`（PARENT）を追記する
