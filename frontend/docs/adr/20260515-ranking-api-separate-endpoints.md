---
date: 2026-05-15
status: decided
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
