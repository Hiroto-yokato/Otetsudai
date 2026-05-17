# 要求内容

## 概要

子ども（CHILD ロール）が未申請（UNPAID）のお手伝いを選択してお小遣いを申請する機能を実装する。申請額はお手伝い回数と日付のパターンに基づく算出ルールで自動計算される。

## 背景

お手伝い登録機能が完成し、子どもが実績を蓄積できるようになった。次のステップとして、未申請のお手伝いをまとめてお小遣い申請する機能が必要。`functional-design.md` の RequestController / AllowanceService / AllowanceRequestRepository に相当する実装を行う。

## 実装対象の機能

### 1. お小遣い申請フォーム（GET /requests/new）

- 未申請（UNPAID）のお手伝い一覧をチェックボックスで表示する
- チェックを変更するたびに AJAX で申請金額をリアルタイムプレビューする
- 「申請する」ボタンで申請を確定する
- 未申請のお手伝いが0件のときは申請できない旨を表示する

### 2. 金額プレビュー（POST /requests/preview）

- 選択した choreId リストを受け取り、申請金額を JSON で返す
- 計算ロジック:
  - 日付ごとにグループ化
  - n = 1: ¥20 / n = 2: ¥50 / n ≥ 3: ¥50 × (n - 1)
  - 全日付の合計が申請金額

### 3. 申請確定（POST /requests）

- 選択した choreId リストで AllowanceRequest を作成する（status = PENDING）
- 選択した Chore の status を UNPAID → PENDING に変更する
- Chore.requestId に AllowanceRequest.id をセットする
- 申請成功後は `/chores?applied=true` にリダイレクトする

## 受け入れ条件

### お小遣い申請フォーム
- [ ] CHILD ロールでログインすると `/requests/new` にアクセスできること
- [ ] UNPAID のお手伝いがチェックボックスで表示されること
- [ ] チェックを変更するたびに金額プレビューが更新されること
- [ ] UNPAID が0件のとき「申請できるお手伝いがありません」と表示されること

### 金額計算
- [ ] 1日1件: ¥20
- [ ] 1日2件: ¥50
- [ ] 1日3件以上: ¥50 × (n - 1)
- [ ] 複数日の合算が正しいこと

### 申請確定
- [ ] 申請後に AllowanceRequest が PENDING で作成されること
- [ ] 申請した Chore が PENDING になること
- [ ] 申請した Chore の requestId に AllowanceRequest.id がセットされること
- [ ] 何も選択せずに申請するとエラーメッセージが表示されること
- [ ] 申請成功後は `/chores?applied=true` にリダイレクトされること

### お手伝い一覧（chore/list.html 更新）
- [ ] `?applied=true` のとき申請完了メッセージが表示されること
- [ ] 「お小遣いを申請する」ボタンが表示されること（UNPAID がある場合）

## スコープ外

以下はこのフェーズでは実装しません:

- 親による承認・却下（次フェーズの ApprovalController）
- 却下後の UNPAID 戻し処理（承認機能と同時に実装）
- 申請履歴の閲覧（子ども側）

## 参照ドキュメント

- `docs/functional-design.md` — AllowanceService / RequestController / AllowanceRequestRepository の定義
- `docs/functional-design.md` — お小遣い計算アルゴリズム
- `docs/architecture.md` — レイヤードアーキテクチャの制約
