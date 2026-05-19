---
name: fix
description: 버그를 재현 테스트 → 수정 → 통과 사이클로 해결한다. 수정 완료 후 /commit을 호출한다.
argument-hint: "[버그 설명] [--skip-repro: 재현 테스트 없이 바로 수정]"
allowed-tools: Read, Glob, Grep, Write, Edit, Bash, Agent
---

# fix

버그를 재현 가능한 테스트로 먼저 증명한 뒤 수정한다.
`--skip-repro` 플래그가 있으면 재현 테스트 작성 없이 Step 2부터 시작한다.

## Step 1: 버그 재현 테스트 작성

`$ARGUMENTS`에서 버그 설명을 파악한다. 모호하면 사용자에게 재현 조건을 질문한다.

1. `tags` 파일로 관련 클래스·메서드 위치를 조회한다
2. 기존 테스트 파일을 확인해 테스트 패턴을 파악한다
3. **실패하는 테스트**를 먼저 작성한다 (Green이 아닌 Red 상태)
4. 테스트를 실행해 실제로 실패하는지 확인한다:

```bash
./gradlew test --tests "coffeeshout.해당패키지.해당테스트클래스" 2>&1 | tail -5
```

실패가 확인되면 "재현 테스트 통과 (Red)" 메시지를 출력하고 Step 2로 넘어간다.

## Step 2: 근본 원인 파악

1. 스택 트레이스와 실패 메시지를 분석한다
2. `tags`로 관련 코드 위치를 확인한다
3. 원인을 한 줄로 요약해 사용자에게 설명한다

## Step 3: 최소 범위 수정

- 버그를 유발하는 코드만 수정한다. 주변 코드를 함께 정리하지 않는다
- 수정 범위가 예상보다 크면 사용자에게 알리고 확인을 받은 뒤 진행한다

## Step 4: 테스트 통과 확인

```bash
./gradlew test --tests "coffeeshout.해당패키지.*" 2>&1 | tail -5
```

- 재현 테스트가 Green이 되어야 한다
- 같은 패키지의 기존 테스트가 모두 통과해야 한다

실패 시 `build/test-results/**/*.xml`에서 `<failure>` 또는 `<error>` 태그를 포함한 파일만 찾아 원인을 파악한다.

## Step 5: 커밋

`/commit`을 호출한다. 커밋 메시지 형식: `fix(scope): 버그 설명`
