# お手伝いポイント管理アプリ

子どもがお手伝いの実績を記録し、親がお小遣いを承認する家庭内DXアプリ。

## 機能概要

- 子どもがお手伝い内容・日付を登録
- 子どもが未払いお手伝いに対してお小遣い申請（1回=¥20、2回=¥50、3回目以降+¥50）
- 親が申請を承認または却下し、支給履歴を管理
- 親・子どもで使える機能が異なるロールベース認証

## 技術スタック

- Java 21（LTS）
- Spring Boot 3.4.5
- Thymeleaf（テンプレートエンジン）
- SQLite（DB）
- Maven

## ビルド・実行

```bash
# ビルド
mvn clean install

# 開発サーバー起動
mvn spring-boot:run

# テスト実行
mvn test
```

## ライセンス

LICENSE.txt を参照してください。

---

## 出典・流用について

- **`CLAUDE.md`** は書籍『実践Claude Code入門』のサンプルコード（[chapter8](https://github.com/GenerativeAgents/claude-code-book-chapter8)）の CLAUDE.md をベースに、技術スタックをこのプロジェクト向けに変更したものです。
- **`.claude/` 配下**（スキル・コマンド・エージェント等）は同書籍のサンプルコードを流用しています。

サンプルソースリポジトリ: https://github.com/GenerativeAgents/claude-code-book-chapter8
