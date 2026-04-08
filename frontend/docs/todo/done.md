# 완료

---

## 건의사항 POST /reports API 연동

- `SuggestionTab.tsx`에서 `useMutation`으로 연동
- `IdentifierProvider.clearIdentifier()`에서 `LAST_JOIN_CODE` localStorage 저장
- BUG 카테고리일 때만 `gameType`, `joinCode` 포함, 그 외 `null`
- 에러 시 Toast 표시 (`errorDisplayMode: 'toast'`)
