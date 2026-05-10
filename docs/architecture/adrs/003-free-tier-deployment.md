# 003: Free tier deployment

## 状態

採用

## 背景

ArticleShelf はポートフォリオとして公開しやすく、個人利用や小規模検証で運用しやすい構成を優先する。
商用 SLA や高可用性よりも、無料枠での公開、運用コストの低さ、構成の説明しやすさを重視する。

詳細な公開構成と運用手順は [デプロイ構成](../../deployment/README.md) を正本とする。

## 決定

- frontend は Cloudflare Pages で静的配信する
- backend API は Render Free Web Service で公開する
- DB は Supabase Free PostgreSQL を使う
- CI は GitHub Actions が担当する
- Render Free Web Service の休眠抑制は Cloudflare Worker から 10 分ごとに health check を送る運用にする
- Cloudflare Pages / Render の Git 連携による auto deploy を使う

## 代替案

- すべてを単一 VPS に載せる: 自由度は高いが、OS / network / security patch の運用負荷が増える
- 有料 PaaS / managed DB を使う: cold start や制限は減るが、ポートフォリオ段階では継続コストが増える
- CI の scheduled workflow に運用ジョブを載せる: CI と runtime 運用の責務が混ざるため、現在は Cloudflare Worker に分離する

## トレードオフ

- 無料枠は容量制限、休眠、起動遅延、利用条件変更の影響を受ける
- Render Free の cold start は完全にはなくならないため、初回アクセス時の待機表示や health check を考慮する
- CI と定期 ping を分けることで責務は明確になるが、Cloudflare Worker 側の運用確認が必要になる

## 今後

- 利用量や可用性要件が上がった場合は、backend / DB の有料プランや別構成を検討する
- pricing / docs の変更に合わせて公開構成を見直す
- 監視、metrics、rollback 手順を強化する
