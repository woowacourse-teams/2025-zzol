# API 연동 TODO

## 건의사항/신고 API (미구현)

### 필요한 엔드포인트

```
POST /reports
Content-Type: application/json

{
  "category": "BUG" | "SUGGESTION" | "OTHER",
  "gameType": "CARD_GAME" | "RACING_GAME" | ... | null,  // 버그 신고일 때만
  "content": string
}
```

### 현재 상태

프론트에서 제출 버튼 클릭 시 `console.log`만 출력하고 성공 UI 표시.

### 연동 시 변경할 파일

- `src/features/home/components/SuggestionTab/SuggestionTab.tsx`
  - `handleSubmit` 함수 내 `console.log` 제거
  - `useMutation` 훅으로 교체

```typescript
// 변경 전
const handleSubmit = () => {
  console.log({ category, gameType, content });
  setStep('success');
};

// 변경 후
const { execute: submitReport, loading } = useMutation({
  endpoint: '/reports',
  method: 'POST',
  onSuccess: () => setStep('success'),
});

const handleSubmit = () => {
  submitReport({ category, gameType, content });
};
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
