# 완료

---

## 비로그인 시 `/users/me` 불필요 호출 제거

- `AuthProvider` bootstrap에서 `hasToken || !isDev` → `hasToken` 으로 단순화
- `CookieTokenStore` → `LocalStorageTokenStore` 교체 후 남아있던 스테일 조건 및 주석 제거
- 프로덕션 빌드에서 비로그인 사용자도 무조건 `/users/me`를 호출해 백엔드 WARN 로그가 발생하던 문제 수정

---

## DevTools 기능 가드를 `NODE_ENV` → `ENABLE_DEVTOOLS`로 통일

- `useMockMode`, `useServiceWorkerUpdate`의 `isDev (NODE_ENV==='development')` 가드를 `isDevToolsEnabled (ENABLE_DEVTOOLS)` 로 교체
- 기존에는 `ENABLE_DEVTOOLS=true`로 패널이 노출되는 환경(be/dev 등)에서도 `NODE_ENV`가 `'production'`이면 업데이트 배너·대시보드 Mock 토글이 no-op 처리됐음
- 이후 두 기능은 devtools 패널 노출 조건(`ENABLE_DEVTOOLS`)과 동일한 기준으로 활성화

---

## 블록 쌓기 TOP 플레이어 랭킹 탭 연동 (#1161)

- `rankingConfigs.ts`에 `blockstacking-top-players` 카테고리 추가 (`GET /dashboard/blockstacking-top-players`)
- `dashBoard.ts`에 `BlockStackingTopPlayer` 타입 추가
- 랭킹 탭 아코디언 UI 및 빈 배열 처리는 기존 `RankingAccordionItem` 인프라 재사용

---

## 건의사항 POST /reports API 연동

- `SuggestionTab.tsx`에서 `useMutation`으로 연동
- `IdentifierProvider.clearIdentifier()`에서 `LAST_JOIN_CODE` localStorage 저장
- BUG 카테고리일 때만 `gameType`, `joinCode` 포함, 그 외 `null`
- 에러 시 Toast 표시 (`errorDisplayMode: 'toast'`)
