# Thumbnail cache eviction 導入

## 状態

未対応

## 優先度

P3

## 目的

IndexedDB の thumbnail cache に最大件数や最大容量の上限を設け、長期利用で外部画像 cache が溜まり続けないようにする。

## 対象

- `frontend/src/shared/services/thumbnailCache.ts`
- IndexedDB thumbnail records
- failure cache
- thumbnail lazy loading
- performance / storage docs

## 対応内容

- thumbnail cache の現行保存件数 / サイズ管理を確認する
- 最大件数、最大合計サイズ、`cachedAt` による LRU 削除方針を検討する
- cache eviction を実装する
- failure cache の TTL と eviction の関係を整理する
- CORS 非対応画像を backend proxy で扱うか、安全側に表示しないかの方針を docs に残す

## 完了条件

- thumbnail cache に上限または eviction 方針がある
- 古い cache record が削除される
- thumbnail lazy loading と object URL revoke の既存挙動が維持されている
- unit tests と performance / architecture docs が更新されている

## 根拠

Frontend レビューで、IndexedDB cache、pendingLoads、failure cache は良いが、総量上限や古い record 削除がなく、長く使うとサムネイルが溜まり続けると指摘されたため。
