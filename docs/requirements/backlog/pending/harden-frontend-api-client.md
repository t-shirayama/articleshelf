# Frontend API client 強化

## 状態

未対応

## 優先度

P2

## 目的

`shared/api/client.ts` の refresh retry、CSRF header、error mapping の強みを維持しつつ、request cancellation、production env validation、testability を高める。

## 対象

- `frontend/src/shared/api/client.ts`
- auth refresh integration
- `VITE_API_BASE_URL`
- search / route transition requests
- frontend API client tests

## 対応内容

- `request<T>` に `AbortSignal` を渡せるようにする
- 検索や route transition で古い request を中断できる設計を検討する
- production build で `VITE_API_BASE_URL` 未設定を検出する
- module global な `accessToken` / `refreshAccessToken` / `refreshPromise` の扱いを整理する
- 必要に応じて `createApiClient` または `resetApiClientForTest` を導入する

## 完了条件

- API request cancellation の利用方針が決まっている
- production build の API base URL 誤設定に気づける
- API client の token / refresh state がテストしやすい形になっている
- 既存の refresh retry、CSRF header、Accept-Language、error mapping が維持されている
- architecture / deployment / testing docs が必要に応じて更新されている

## 根拠

Frontend レビューで、`shared/api/client.ts` は強い設計だが、AbortController、production env validation、module scope state の testability を改善すると大規模 SPA としての品質が上がると指摘されたため。
