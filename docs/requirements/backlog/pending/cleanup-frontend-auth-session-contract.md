# Frontend 認証 session contract 整理

## 状態

未対応

## 優先度

P2

## 目的

Frontend の認証状態管理、proactive refresh timer、JWT decode helper、displayName validation を整理し、Backend の認証仕様と UI 契約を合わせる。

## 対象

- `frontend/src/features/auth/stores/auth.ts`
- `frontend/src/features/auth/components/AuthScreen.vue`
- `frontend/src/features/auth/api/authApi.ts`
- JWT exp decode
- proactive refresh timer
- displayName registration validation
- logout all UX

## 対応内容

- proactive refresh timer を module scope から store instance または dedicated service へ寄せる
- `readJwtExp` を `shared/auth/jwt.ts` などの純粋 helper に分離する
- 登録時の `displayName` を必須にするか任意にするかを Backend 仕様と合わせる
- `logoutAll` 失敗時も local credentials を消す fail closed の意図をコメントまたは docs に残す
- auth store tests を新しい責務分離に追従する

## 完了条件

- proactive refresh timer の lifecycle が store / service に閉じている
- JWT exp decode helper が単体テスト可能になっている
- Frontend と Backend の displayName 必須 / 任意仕様が一致している
- logout all 失敗時の UX と security 意図が明確になっている
- auth specs / frontend architecture docs が必要に応じて更新されている

## 根拠

Frontend レビューで、proactive refresh timer が module scope にあること、JWT decode helper を分離できること、Frontend では displayName 必須だが Backend は未指定時 username を使うため契約差があることを指摘されたため。
