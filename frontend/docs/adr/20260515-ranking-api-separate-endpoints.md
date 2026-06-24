---
date: 2026-05-15
status: amended
amended: 2026-05-23
---

# 랭킹 대시보드 API를 통합하지 않고 별도 엔드포인트 유지

## 맥락

홈 화면 랭킹 탭은 세 개의 엔드포인트를 각각 호출한다:

- `GET /dashboard/top-winners`
- `GET /dashboard/lowest-probability-winner`
- `GET /dashboard/game-play-counts`

이를 하나의 `GET /dashboard/summary` 같은 통합 엔드포인트로 합치는 방안이 검토됐다.

## 결정

별도 엔드포인트를 유지한다. 통합하지 않는다.

## 고려한 대안

- **통합 엔드포인트 `/dashboard/summary`**: 백엔드 구현 공수가 필요하다. 현행 API가 이미 올바르게 동작하고 있어 변경 대비 효과가 낮다.

## 결과 및 영향

- 프론트엔드에서 탭별로 독립 요청을 보내므로 일부 탭 응답이 느려도 다른 탭에 영향을 주지 않는다.
- 향후 통합이 필요해질 경우 `src/features/home/config/rankingConfigs.ts` 의 각 항목 `endpoint` 필드만 교체하면 된다. 컴포넌트 변경 없음.
- 새로운 랭킹 카테고리는 `rankingConfigs.ts`에 항목을 추가하는 방식으로 확장한다.

## 갱신 이력 (2026-05-23)

랭킹 탭에서 `top-winners`, `lowest-probability-winner`, `game-play-counts` 세 카테고리를 제거했다. 다른 대시보드 슬라이드(`TopWinnersSlide`, `LowestProbabilitySlide`)와 내용이 중복되어 UX 관점에서 불필요하다고 판단했기 때문이다.

**현재 `rankingConfigs.ts`에 남은 카테고리:**

- `blockstacking-top-players` — `GET /dashboard/block-stacking-top-players`
- `racing-game-top-players` — `GET /dashboard/racing-game-top-players`

**제거된 세 엔드포인트의 현재 소비처:**

- `top-winners`, `lowest-probability-winner` → `src/features/home/hooks/useDashboardData.ts` (DashBoard 슬라이드)
- `game-play-counts` → 현재 미사용. 백엔드 엔드포인트는 유지 중.

별도 엔드포인트 유지 결정 자체는 유효하다. `rankingConfigs.ts` 확장 패턴도 동일하게 적용된다.
