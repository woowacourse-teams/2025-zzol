---
name: ci-fix
description: PR의 CI 실패 테스트를 자동으로 감지하고 수정 사이클을 진행한다. 수정 완료 후 /commit을 호출한다.
argument-hint: "[PR번호] (생략 시 현재 브랜치의 PR 자동 탐지)"
allowed-tools: Read, Glob, Grep, Write, Edit, Bash, Agent
---

# ci-fix

PR 번호를 받아 GitHub CI "Backend Test Results" 체크의 실패 테스트를 자동으로 감지하고 수정한다.

## Step 1: PR 및 CI 런 확인

`$ARGUMENTS`에서 PR 번호를 파싱한다. 없으면 현재 브랜치로 자동 탐지한다.

```bash
# PR 번호가 없을 때 — 현재 브랜치로 탐지
gh pr view --json number -q .number

# 체크 상태 확인
gh pr checks <PR번호> --json name,status,conclusion 2>/dev/null
```

`Backend Test Results` 체크가 `failure`가 아니면 "CI가 통과 상태입니다"를 출력하고 종료한다.

## Step 2: 실패 테스트 추출

### 2-1. Check Annotations API (우선 시도)

```bash
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
HEAD_SHA=$(gh pr view <PR번호> --json headRefOid -q .headRefOid)

# "Backend Test Results" 체크 런 ID 조회
CHECK_RUN_ID=$(gh api "repos/${REPO}/commits/${HEAD_SHA}/check-runs" \
  --jq '.check_runs[] | select(.name == "Backend Test Results") | .id')

# Annotations 조회 (최대 100개)
gh api "repos/${REPO}/check-runs/${CHECK_RUN_ID}/annotations" \
  --jq '.[] | "\(.title)\n  경로: \(.path):\(.start_line)\n  메시지: \(.message)\n"'
```

Annotation이 0개면 Step 2-2로 넘어간다.

### 2-2. CI 런 로그 폴백

```bash
RUN_ID=$(gh run list --branch $(gh pr view <PR번호> --json headRefName -q .headRefName) \
  --workflow backend-ci.yml --limit 1 --json databaseId -q '.[0].databaseId')

gh run view ${RUN_ID} --log-failed 2>/dev/null | \
  grep -E "(FAILED|MethodSource|expected:|but was:)" | head -80
```

## Step 3: 실패 목록 정리

추출한 실패를 다음 형식으로 출력한다:

```
[실패 1] coffeeshout.room.domain.RoomTest > 방_제목이_빈문자열이면_예외
  원인: expected: <IllegalArgumentException> but was: <null>
  파일: room/src/test/java/coffeeshout/room/domain/RoomTest.java:42

총 N개 실패 감지. 순서대로 수정하겠습니다.
```

실패 목록이 비어 있으면 "Annotations에서 실패 정보를 가져올 수 없습니다. 로그를 직접 확인해주세요."를 출력하고 종료한다.

## Step 4: 테스트별 수정 사이클

각 실패 테스트에 대해 반복한다.

### 4-1. 원인 분석

1. `tags`로 관련 클래스·메서드 위치 조회
2. 실패한 테스트 파일과 대응 프로덕션 코드를 확인한다
3. 원인을 한 줄로 요약한다

### 4-2. 최소 범위 수정

- 수정 범위가 예상보다 크거나(3개 파일 초과) 타 도메인에 영향이 있으면 사용자에게 확인을 받는다
- 테스트 코드 자체가 잘못된 경우(잘못된 기댓값, 환경 가정 오류)도 수정 대상이다

### 4-3. 단일 테스트 통과 확인

```bash
./gradlew test --tests "coffeeshout.해당패키지.해당테스트클래스" 2>&1 | tail -5
```

실패 시 `build/test-results/**/*.xml`에서 `<failure>` 또는 `<error>` 태그를 포함한 파일만 찾아 원인을 재분석한다. 2회 이상 실패 시 사용자에게 보고하고 다음 처리 방법(계속 수정 / 이 테스트 건너뜀 / 전체 중단)을 선택하게 한다.

## Step 5: 전체 회귀 확인

모든 개별 수정 완료 후 영향 받은 모듈 전체를 한 번 더 테스트한다:

```bash
./gradlew test 2>&1 | tail -10
```

실패 시 Step 4와 동일하게 XML을 분석한다.

## Step 6: 커밋

`/commit`을 호출한다.
