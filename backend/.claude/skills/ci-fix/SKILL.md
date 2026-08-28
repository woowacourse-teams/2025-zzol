---
name: ci-fix
description: PR의 CI 실패 테스트를 자동으로 감지하고 수정 사이클을 진행한다. 수정 완료 후 /commit을 호출한다.
argument-hint: "[PR번호] (생략 시 현재 브랜치의 PR 자동 탐지)"
allowed-tools: Read, Glob, Grep, Write, Edit, Bash, Agent
---

# ci-fix

PR 번호를 받아 GitHub Actions `backend-ci.yml` 런 로그에서 실패한 테스트를 자동으로 감지하고 수정한다.

## Step 1–2: 실패 테스트 추출

`fetch-failures.sh`로 PR 상태 확인 및 실패 목록을 가져온다. 이 스크립트는 "Backend Test Results" 체크런의 실패 annotation을 먼저 시도하고, 체크런이 없거나 annotation이 비어 있으면 PR head 브랜치의 최근 `backend-ci.yml` 런 로그(gradle JUnit 출력)를 직접 파싱하는 것으로 폴백한다(#1521, #1564):

```bash
bash .claude/skills/ci-fix/fetch-failures.sh <PR번호>
```

- "CI가 통과 상태입니다" 출력 시 종료한다
- "CI가 아직 실행 중입니다" 출력 시 완료를 기다렸다가 재시도한다
- "테스트 실패 라인을 찾지 못했습니다" 출력 시(컴파일·인프라 등 테스트 외 실패) 함께 출력된 로그 마지막 부분으로 원인을 판단한다

## Step 3: 실패 목록 정리

추출한 실패를 아래 형식으로 요약해 출력한다:

```text
[실패 1] coffeeshout.room.domain.RoomTest > 방_제목이_빈문자열이면_예외
  원인: expected: <IllegalArgumentException> but was: <null>
  파일: room/src/test/java/coffeeshout/room/domain/RoomTest.java:42

총 N개 실패 감지. 순서대로 수정하겠습니다.
```

## Step 4: 테스트별 수정 사이클

각 실패 테스트에 대해 반복한다.

1. 실패한 테스트 파일과 대응 프로덕션 코드를 확인하고 원인을 한 줄로 요약한다
2. 수정 범위가 3개 파일 초과(그 이상이면 설계 문제일 가능성이 높아 사람의 판단이 필요하다)거나 타 도메인에 영향이 있으면 사용자에게 확인을 받는다
3. 테스트 코드 자체가 잘못된 경우(잘못된 기댓값, 환경 가정 오류)도 수정 대상이다
4. `./gradlew test --tests "coffeeshout.해당패키지.해당테스트클래스" --build-cache` 로 단일 테스트 통과 확인
   - 실패 시 `**/build/test-results/**/*.xml` 에서 `<failure>` 또는 `<error>` 파일을 찾아 재분석
   - 2회 이상 실패 시 사용자에게 보고하고 처리 방법(계속 수정 / 건너뜀 / 중단)을 선택하게 한다

## Step 5: 전체 회귀 확인

모든 수정 완료 후 `/run-tests`로 영향 받은 모듈 전체를 확인한다(전체 회귀는 `./gradlew test`를 직접 실행하지 않고 위임한다. 근거는 CLAUDE.md 테스트 실행 규칙). 실패 시 Step 4와 동일하게 XML을 분석한다.

## Step 6: 커밋

`/commit`을 호출한다.
