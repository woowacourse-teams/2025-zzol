# 완료

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
