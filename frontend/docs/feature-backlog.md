# Feature Backlog

구현 보류 중인 기능 목록. 우선순위와 접근 방향이 결정되면 이 파일에서 해당 항목을 제거하고 작업을 시작한다.

---

## 게임 추가 게시판

**현재 상태**: 건의사항 탭 내 "게임 추가" 카테고리로 폼 제출 처리 중.

**보류 이유**: 별도 게시판(게임 추가 요청 목록 조회 + 좋아요/추천 기능)으로 발전시킬지 검토 중.

**구현 시 고려사항**:
- 게시판 라우트 추가 (`/board/game-requests`)
- 게시글 목록 API, 추천 API 필요
- 현재 `SuggestionTab`의 `GAME_REQUEST` 카테고리를 게시판 링크로 교체

---

## 랭킹 API 통합 엔드포인트

**현재 상태**: `/dashboard/top-winners`, `/dashboard/lowest-probability-winner`, `/dashboard/game-play-counts` 각각 별도 호출.

**보류 이유**: 현행 API로 충분히 동작하므로 백엔드 공수 대비 우선순위 낮음.

**구현 시 변경 파일**: `src/features/home/config/rankingConfigs.ts`의 각 카테고리 `endpoint` 필드만 교체하면 됨. `transformData`는 응답 형식에 맞게 수정.

---

## 최근 사용 닉네임 — 자동 생성 닉네임 포함 여부

**현재 상태**: 방 입장 성공 시에만 닉네임 저장. 자동 생성 닉네임은 저장 안 됨.

**고려사항**: 자동 생성 후 방에 입장하면 어차피 저장되므로 현행 유지가 합리적. 별도 저장 로직 불필요.
