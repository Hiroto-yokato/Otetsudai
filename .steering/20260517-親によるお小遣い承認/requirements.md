# 要求内容

## 概要

親（PARENT ロール）が子どもの申請（PENDING）を一覧で確認し、承認または却下する機能を実装する。承認時は対象お手伝いの status が APPROVED に、却下時は UNPAID に戻り再申請可能になる。

## 背景

お小遣い申請機能が完成し、子どもが PENDING 状態の申請を作成できるようになった。次のステップとして、親が申請一覧を確認して承認・却下し、支給履歴を管理する機能が必要。`functional-design.md` の ApprovalController / AllowanceService（承認系）に相当する実装を行う。

## 実装対象の機能

### 1. 申請一覧（GET /approvals）
- PENDING 状態の申請を一覧表示する（申請日・金額・子ども ID を表示）
- 申請がない場合は「申請がありません」と表示する
- 各申請をクリックすると詳細ページに遷移する

### 2. 申請詳細・承認/却下（GET /approvals/{id}, POST /approvals/{id}/approve, POST /approvals/{id}/reject）
- 申請の詳細情報（申請日、金額、お手伝い明細）を表示する
- 「承認する」ボタンで承認処理を実行する
- 「却下する」フォームで却下理由を入力して却下処理を実行する

### 3. 承認処理（POST /approvals/{id}/approve）
- AllowanceRequest.status を PENDING → APPROVED に変更する
- AllowanceRequest.approvedAt に現在日時をセットする
- 対象 Chore.status を PENDING → APPROVED に変更する
- 処理後は /approvals にリダイレクトする

### 4. 却下処理（POST /approvals/{id}/reject）
- AllowanceRequest.status を PENDING → REJECTED に変更する
- AllowanceRequest.rejectionReason に却下理由をセットする
- 対象 Chore.status を PENDING → UNPAID に戻し、Chore.requestId を null に戻す
- 処理後は /approvals にリダイレクトする

### 5. 支給履歴（GET /history）
- APPROVED 状態の申請を一覧表示する（承認日・金額・子ども ID）
- 承認日時の降順で表示する

## 受け入れ条件

### 申請一覧
- [ ] PARENT ロールでログインすると `/approvals` にアクセスできること
- [ ] PENDING の申請が一覧表示されること
- [ ] 申請がない場合は適切なメッセージが表示されること

### 承認処理
- [ ] 「承認する」ボタンを押すと AllowanceRequest が APPROVED になること
- [ ] 「承認する」ボタンを押すと対象 Chore が APPROVED になること
- [ ] 承認後は /approvals にリダイレクトされること

### 却下処理
- [ ] 却下理由を入力して「却下する」ボタンを押すと AllowanceRequest が REJECTED になること
- [ ] 却下後は対象 Chore が UNPAID に戻ること
- [ ] 却下後は対象 Chore の requestId が null に戻ること
- [ ] 却下後は /approvals にリダイレクトされること

### 支給履歴
- [ ] `/history` で APPROVED の申請が一覧表示されること
- [ ] 承認日時の降順で表示されること

## スコープ外

- 申請履歴の子ども側からの閲覧（別フェーズで実装）
- 承認メール通知
- ページネーション（件数が少ないため MVP では省略）
