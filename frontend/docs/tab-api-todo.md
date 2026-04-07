# API 연동 TODO

## 건의사항/신고 API ✅ 완료 (fe/feat/home-tabs)

`src/features/home/components/SuggestionTab/SuggestionTab.tsx`에서 `useMutation`으로 연동 완료.

- `POST /reports` — `{ category, gameType, joinCode, content }`
- BUG 카테고리일 때만 `gameType`, `joinCode` 포함 (그 외 `null`)
- `joinCode`: localStorage `zzol-last-join-code` 값 (없으면 `null`)
- 에러 시 Toast 표시 (`errorDisplayMode: 'toast'`)

---

## 패치 내역 API (미구현)

### 현재 상태

`MenuTab/views/PatchNotesView.tsx`에 정적 데이터 하드코딩.

### 연동 시 변경할 파일

- `src/features/home/components/MenuTab/views/PatchNotesView.tsx`
  - 파일 상단 `PATCH_NOTES` 상수 제거
  - `useFetch` 훅으로 교체

```typescript
// 변경 후 (엔드포인트 확정 시)
const { data, loading } = useFetch<PatchNote[]>('/patch-notes');
```

---

## 랭킹 API 통합 (선택)

### 현재 상태

`/dashboard/top-winners`, `/dashboard/game-play-counts` 각각 별도 엔드포인트 호출.

### 통합 시 변경할 파일

- `src/features/home/config/rankingConfigs.ts`
  - 각 카테고리의 `endpoint` 필드만 교체하면 됨
  - `transformData` 함수는 응답 형식에 맞게 수정

통합 엔드포인트 예시:
```
GET /rankings?category=TOP_WINNERS&limit=5
GET /rankings?category=GAME_PLAY_COUNTS&limit=5
```
