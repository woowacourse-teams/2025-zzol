---
id: 1540
title: dev/prod 단일 브랜치 체계 전환
status: 제안
date: 2026-07-09
scope: 브랜치 전략, .github/workflows(CD·CI), git-push-safety
constraint: 모든 작업은 dev에서 분기·PR. prod 배포는 단일 통합 prod 경로로 일원화하고 be/*·fe/* 레거시 브랜치는 폐기(전환 완료 후)
---

# 1540. dev/prod 단일 브랜치 체계 전환

- 날짜: 2026-07-09
- 상태: 제안 (설계 초안 — 실행은 후속 이슈로 분리, #1540)

## 컨텍스트

브랜치 전략은 이미 "단일 `dev` 통합"으로 문서상 선언돼 있다(루트 CLAUDE.md·git-push-safety). 그러나 실제 브랜치·배포 체계는 전환기 상태로 남아 있다.

- **통합 `prod` 브랜치가 없다.** 프로덕션 배포가 `be/prod`·`fe/prod`로 이원화돼 있고, `backend-cd.yml`은 `be/prod` push를, 프론트 CD는 `fe/prod`를 트리거로 배포한다. `dev` push는 어떤 prod 배포도 유발하지 않는다.
- **그 결과 풀스택·공통 수정이 prod에 바로 반영되지 않는다.** 이번 OAuth 로그인 장애(#1537)에서, `dev`로의 수정(PR #1538)과 별개로 `be/prod` 대상 최소 핫픽스 PR(#1539)을 따로 만들어야 했다.
- **`dev`가 prod 브랜치보다 뒤처져 있어 단순 승격이 불가능하다.** 측정값(2026-07-09):

| 비교 | dev 앞섬 | dev 뒤짐 | 차이 파일 |
| --- | --- | --- | --- |
| dev ↔ be/prod | +19 | -24 | 838 |
| dev ↔ fe/prod | +684 | -440 | 2110 |
| dev ↔ be/dev | +18 | -7 | 836 |
| dev ↔ fe/dev | +684 | -370 | 2110 |

`dev`는 레거시 브랜치들의 상위집합이 아니다. 특히 FE는 2110파일 규모로 크게 갈라져 있어, "dev를 prod로 그대로 민다"가 성립하지 않는다. 자동화 워크플로우(`*-delete-merged-branch`·`close-issue`·`codeql`·`chromatic-*` 등)도 다수가 `be/*`·`fe/*`를 참조한다.

## 결정

**장기 목표를 `dev`(통합 개발) → `prod`(통합 배포) 2-브랜치로 단일화**하고, `be/dev`·`fe/dev`·`be/prod`·`fe/prod` 레거시 4브랜치를 폐기한다. 단, 위 divergence 때문에 **일괄 병합이 아니라 단계적 전환**으로 진행한다.

전환 순서(초안):

1. **reconcile 선행** — `dev`와 `be/prod`·`fe/prod` 사이 양방향 차이를 해소한다. prod에만 있는 커밋(be +24 / fe +440)을 `dev`로 흡수하고, 검증 안 된 대형 변경이 prod로 새어나가지 않도록 범위를 확인한다. FE(2110파일)는 별도 리콘실 세션이 필요하다.
2. **통합 `prod` 생성** — reconcile 완료된 `dev`에서 `prod` 브랜치를 만든다.
3. **배포 트리거 재배선** — `backend-cd`·프론트 CD의 트리거를 `be/prod`·`fe/prod` → `prod`로 바꾼다. 나머지 자동화 워크플로우의 `be/*`·`fe/*` 참조도 `dev`/`prod`로 정리한다.
4. **레거시 브랜치 폐기** — 원격에서 `be/dev`·`fe/dev`·`be/prod`·`fe/prod`를 삭제하고, git-push-safety 보호목록·`backend/.claude/skills/commit/preflight.sh`의 `PROTECTED`에서 제거한다.

이 ADR은 방향과 순서를 **제안**한다. 각 단계는 후속 실행 이슈로 분리한다.

## 고려한 대안

| 대안 | 장점 | 단점 |
| --- | --- | --- |
| 일괄 `dev → be/prod` 병합 | 즉시 단일화 | dev의 미검증 19+커밋·838파일을 prod에 통째 배포 → blast radius 과다, 장애 위험(#1537 논의에서 기각) |
| 전환기 유지(현행) | 추가 작업 없음 | 풀스택/공통 수정마다 이원 배포·핫픽스, dev-prod 표류 지속 |
| **단계적 전환(채택)** | reconcile로 blast radius 통제, 각 단계 검증 가능 | 여러 후속 이슈로 나뉘어 시간 소요, FE reconcile 부담 |

## 트레이드오프

- 단계적 전환은 즉시 끝나지 않는다. 특히 FE의 2110파일 divergence를 reconcile하는 비용이 크다. 그러나 프로덕션에 미검증 릴리스를 흘리지 않는 안전성을 우선한다.
- 전환 완료 전까지는 레거시 브랜치가 보호목록에 남아 이원 체계가 유지된다(호환 목적). 완료 시점에 일괄 정리한다.

## 결과

- 전환 완료 후 개발자는 `dev` 한 곳에서만 분기·PR하고, `prod` 한 곳으로만 배포된다. 풀스택·공통 수정의 이원 핫픽스가 사라진다.
- `.github/workflows`의 브랜치 참조가 `dev`/`prod`로 단순화된다.
- git-push-safety 보호목록이 `dev`·`prod`·`main`으로 축소된다.
- 이 ADR은 루트 `docs/adr/`의 첫 전역 ADR로, 이슈번호 ID(#1540)·생성형 index 규약을 적용한 사례다.
