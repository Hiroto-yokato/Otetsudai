# 設計: 子どものお手伝い一括登録

## エンドポイント

- `GET /chores/bulk` — 一括登録フォーム表示
- `POST /chores/bulk` — 一括登録処理

`/chores/**` は既に CHILD 専用のため SecurityConfig 変更不要。

## DTO: `BulkChoreForm`

```java
public class BulkChoreForm {
    private List<ChoreEntryForm> entries = new ArrayList<>();

    public static class ChoreEntryForm {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate choreDate;
        private String content;  // @NotBlank なし — 空行はサービス層でスキップ
    }
}
```

## サービス: `ChoreService.createChoresBulk`

- entries を走査し、content が空の行はスキップ
- 日付が null の行は当日日付をデフォルトとする
- 有効な行が1件もなければ IllegalStateException
- 有効行を順番に save して返す

## コントローラー: `ChoreController`

```
GET  /chores/bulk → bulkForm(5行・今日の日付) + choreTemplates をモデルに追加 → chore/bulk
POST /chores/bulk → createChoresBulk → redirect:/chores?registered=true
                    バリデーションエラー or IllegalStateException → chore/bulk に戻る
```

## テンプレート: `chore/bulk.html`

- テーブル形式（日付列・内容列）
- `th:each` で entries を描画
- `entries[N].choreDate` / `entries[N].content` の name 属性でバインド
- 「行を追加」ボタン + JavaScript で新行を追加（rowCount を JS 変数で管理）
- 既存 datalist (#choreTemplates) を再利用
- 「まとめて登録する」送信ボタン / 「キャンセル」リンク

## ナビ: `chore/list.html`

- 「+ 登録する」ボタンの隣に「まとめて登録する」リンクを追加

## 変更ファイル一覧

| ファイル | 変更種別 |
|---|---|
| `BulkChoreForm.java` | 新規作成 |
| `ChoreService.java` | 追加: `createChoresBulk` |
| `ChoreController.java` | 追加: `GET/POST /chores/bulk` |
| `chore/bulk.html` | 新規作成 |
| `chore/list.html` | 変更: 「まとめて登録する」リンク追加 |
| `ChoreServiceTest.java` | 追加: `createChoresBulk` テスト |
