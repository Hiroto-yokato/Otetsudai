# 設計: お手伝い登録機能追加

## 1. 編集機能

### エンドポイント
- `GET /chores/{id}/edit` — 編集フォーム表示
- `POST /chores/{id}/edit` — 更新処理

### サービス
- `ChoreService.updateChore(Long choreId, String loginUserId, ChoreForm form)`
  - UNPAID 以外 → IllegalStateException
  - 他人のお手伝い → IllegalStateException
  - 正常 → 保存して返す

### テンプレート
- `chore/edit.html` — new.html と同構造、action を `/chores/{id}/edit` に変更

### list.html 変更
- UNPAID 行に「編集」ボタンを追加（削除ボタンの隣）

---

## 2. 親が子どもの一覧を見られる

### セキュリティ
- `SecurityConfig` に `/chores/family` を PARENT 許可として追加
  - `/chores/**` は CHILD のまま維持
  - `/chores/family` を先に宣言（Spring Security は先勝ち）

### リポジトリ
- `ChoreRepository.findByUserIdInOrderByChoreDateDesc(List<Long> userIds)` を追加

### サービス
- `ChoreService.listFamilyChores(String parentUserId)` → `List<Chore>`
  - 親の familyId で子ども Users を取得
  - 子ども全員の chores を取得して返す
- `ChoreService.buildUserMap(List<Long> userIds)` → `Map<Long, User>`
  - テンプレートで子ども名を表示するためのマップ

### コントローラー
- `GET /chores/family` — PARENT が対象
  - `chores` + `userMap` をモデルに追加

### テンプレート
- `chore/family.html` — 子ども名・日付・内容・状態のテーブル

---

## 3. 内容をプルダウンから選べる

### 実装方針
- `ChoreController` に定数リストを定義して new/edit のモデルに渡す
- テンプレートの `<textarea>` を `<input type="text" list="choreTemplates"> + <datalist>` に変更
- 自由入力も可能にする（datalist は強制選択ではない）

### 選択肢（定数リスト）
掃除機をかける / トイレ掃除 / お風呂掃除 / 食器を洗う / 食器を片付ける /
洗濯物をたたむ / 洗濯物を干す / 料理のお手伝い / ゴミを捨てる / テーブルを拭く

---

## 変更ファイル一覧

| ファイル | 変更種別 |
|---|---|
| `ChoreRepository.java` | 追加: `findByUserIdInOrderByChoreDateDesc` |
| `ChoreService.java` | 追加: `updateChore`, `listFamilyChores`, `buildUserMap` |
| `ChoreController.java` | 追加: `editForm`, `update`, `familyList` + 定数リスト |
| `SecurityConfig.java` | 変更: `/chores/family` に PARENT 許可を追加 |
| `chore/new.html` | 変更: textarea → input + datalist |
| `chore/edit.html` | 新規作成 |
| `chore/list.html` | 変更: 「編集」ボタン追加 |
| `chore/family.html` | 新規作成 |
| `ChoreServiceTest.java` | 追加: updateChore・listFamilyChores テスト |
