# 메뉴 탭 API 설계

> 백엔드 팀과의 협의 기준 문서. 프론트 연동 시 변경 사항은 `docs/tab-api-todo.md` 참고.

---

## 1. 건의사항/신고 제출

### `POST /reports`

#### Request

```http
POST /reports
Content-Type: application/json
```

```json
{
  "category": "BUG",
  "gameType": "CARD_GAME",
  "joinCode": "ABC123",
  "content": "카드게임 결과 화면에서 앱이 멈춰요."
}
```

| 필드       | 타입             | 필수 | 설명                                                                                                                                                         |
| ---------- | ---------------- | ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `category` | `string`         | ✅   | `BUG` \| `SUGGESTION` \| `GAME_REQUEST` \| `OTHER`                                                                                                           |
| `gameType` | `string \| null` | ❌   | `category=BUG`일 때만 의미 있음. `CARD_GAME` \| `RACING_GAME` \| `SPEED_TOUCH` \| `BLIND_TIMER` \| `BOMB_RELAY` \| `BLOCK_STACKING` \| `null` (게임 외 버그) |
| `joinCode` | `string \| null` | ❌   | `category=BUG`일 때만 포함. 사용자가 마지막으로 참여한 방 코드. 서버 측 게임 세션 로그 조회에 활용                                                           |
| `content`  | `string`         | ✅   | 1자 이상 200자 이하                                                                                                                                          |

**카테고리별 포함 필드 정리:**

| category       | gameType            | joinCode            | content |
| -------------- | ------------------- | ------------------- | ------- |
| `BUG`          | 있을 수도 없을 수도 | 있을 수도 없을 수도 | ✅      |
| `SUGGESTION`   | 없음                | 없음                | ✅      |
| `GAME_REQUEST` | 없음                | 없음                | ✅      |
| `OTHER`        | 없음                | 없음                | ✅      |

#### Response

**성공 `201 Created`**

```json
{
  "reportId": 42
}
```

응답 body가 부담스럽다면 빈 `201`도 무방. 프론트는 `onSuccess` 콜백에서 `step='success'`로 전환하며 응답 값을 사용하지 않음.

**실패**

| 상태 코드 | 상황                                                             | `message` 예시                               |
| --------- | ---------------------------------------------------------------- | -------------------------------------------- |
| `400`     | content 비어 있음 / 200자 초과 / 유효하지 않은 category·gameType | `"content는 1자 이상 200자 이하여야 합니다"` |
| `429`     | 단시간 내 동일 IP 중복 제출 (선택 구현)                          | `"잠시 후 다시 시도해주세요"`                |
| `500`     | 서버 내부 오류                                                   | `"서버 오류가 발생했습니다"`                 |

에러 응답 envelope:

```json
{
  "status": 400,
  "detail": "content는 1자 이상 200자 이하여야 합니다",
  "errorCode": "INVALID_CONTENT",
  "timestamp": "2025-07-01T12:00:00Z"
}
```

> 프론트 `apiRequest.ts`는 에러 응답에서 `detail` 필드를 읽어 `ApiError.message`에 넣고 Toast로 표시함 (`errorData.detail`, line 69). `detail`에 한국어 메시지를 담아주면 됨.

---

## 2. 패치 내역 조회

### `GET /patch-notes`

#### Request

```http
GET /patch-notes
```

쿼리 파라미터 없음. 전체 목록을 최신순으로 반환.

#### Response

**성공 `200 OK`**

```json
[
  {
    "version": "v1.2.0",
    "releasedAt": "2025-07-01",
    "changes": [
      { "type": "NEW", "description": "하단 메뉴 탭 추가 — 건의사항, 패치 내역, 서비스 정보" },
      { "type": "IMPROVE", "description": "홈 탭 UI 개편 및 레이아웃 일관성 개선" }
    ]
  },
  {
    "version": "v1.1.0",
    "releasedAt": "2025-06-15",
    "changes": [
      { "type": "NEW", "description": "스피드 터치 미니게임 추가" },
      { "type": "FIX", "description": "QR 스캔 후 방 입장 오류 수정" }
    ]
  }
]
```

| 필드                    | 타입     | 설명                                              |
| ----------------------- | -------- | ------------------------------------------------- |
| `version`               | `string` | 버전 문자열. `v{major}.{minor}.{patch}` 형식 권장 |
| `releasedAt`            | `string` | ISO 8601 날짜 (`YYYY-MM-DD`)                      |
| `changes[].type`        | `string` | `NEW` \| `FIX` \| `IMPROVE`                       |
| `changes[].description` | `string` | 변경 내용 한 줄 요약                              |

**실패**

| 상태 코드 | 상황           |
| --------- | -------------- |
| `500`     | 서버 내부 오류 |

---

## 공통 에러 응답 형식

```json
{
  "status": 400,
  "detail": "사용자에게 표시할 메시지",
  "errorCode": "INVALID_CONTENT",
  "timestamp": "2025-07-01T12:00:00Z"
}
```

- `detail`: 한국어로 작성. 프론트 `apiRequest.ts`가 이 필드를 파싱해 Toast에 직접 노출함
- `errorCode`: 에러 분류용 코드 (프론트에서 현재 사용하지 않으나 추후 에러별 분기 처리에 활용 가능)
- `status`, `timestamp`: 서버 공통 포맷 그대로 유지

---

## 프론트 연동 예정 코드

### 건의사항 제출 (`SuggestionTab.tsx`)

```typescript
type ReportRequest = {
  category: 'BUG' | 'SUGGESTION' | 'GAME_REQUEST' | 'OTHER';
  gameType?: string | null;
  joinCode?: string | null;
  content: string;
};

const { mutate: submitReport, loading } = useMutation<void, ReportRequest>({
  endpoint: '/reports',
  method: 'POST',
  errorDisplayMode: 'toast',
  onSuccess: () => setStep('success'),
});

const handleSubmit = () => {
  const lastJoinCode = storageManager.getItem(STORAGE_KEYS.LAST_JOIN_CODE, 'localStorage');
  submitReport({
    category,
    gameType,
    content,
    ...(category === 'BUG' && lastJoinCode ? { joinCode: lastJoinCode } : {}),
  });
};
```

### 패치 내역 조회 (`PatchNotesView.tsx`)

```typescript
type PatchNoteItem = {
  type: 'NEW' | 'FIX' | 'IMPROVE';
  description: string;
};

type PatchNote = {
  version: string;
  releasedAt: string;
  changes: PatchNoteItem[];
};

const { data: patchNotes, loading } = useFetch<PatchNote[]>('/patch-notes');
```
