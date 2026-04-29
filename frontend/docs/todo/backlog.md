# 보류

우선순위 미확정이거나 방향 검토 중인 작업 목록.
진행 결정 시 `planned.md`로, 완료 시 `done.md`로 이동한다.

---

## 게임 추가 게시판

**현재 상태**: 건의사항 탭 내 `GAME_REQUEST` 카테고리로 폼 제출 처리 중.

**보류 이유**: 별도 게시판(게임 추가 요청 목록 조회 + 좋아요/추천)으로 발전시킬지 검토 중.

**구현 시 고려사항**:

- 게시판 라우트 추가 (`/board/game-requests`)
- 게시글 목록 API, 추천 API 필요
- 현재 `SuggestionTab`의 `GAME_REQUEST` 카테고리를 게시판 링크로 교체

---

## 랭킹 API 통합 엔드포인트

**현재 상태**: `/dashboard/top-winners`, `/dashboard/lowest-probability-winner`, `/dashboard/game-play-counts` 각각 별도 호출.

**보류 이유**: 현행 API로 충분히 동작하므로 백엔드 공수 대비 우선순위 낮음.

**구현 시 변경 파일**: `src/features/home/config/rankingConfigs.ts`의 `endpoint` 필드만 교체하면 됨.
