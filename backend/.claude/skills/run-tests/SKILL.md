---
name: run-tests
description: 테스트를 에이전트에 위임해 실행하고 실패 분석 결과를 반환한다.
argument-hint: "[:module | package.pattern] [--sync]"
allowed-tools: Agent
---

# run-tests

테스트 실행을 에이전트에 위임한다. 기본은 백그라운드(비동기)이며, `--sync` 플래그를 붙이면 결과를 기다린다.

## Step 1: 인자 파싱

`$ARGUMENTS`에서 `--sync` 플래그와 대상을 분리한다.

| 대상 입력                    | 실행 명령                                        |
|--------------------------|----------------------------------------------|
| `:module` (콜론으로 시작)      | `./gradlew :module:test`                     |
| `package.pattern` (점 표기) | `./gradlew test --tests "package.pattern"`   |
| 비어 있음                    | `./gradlew test`                             |

점 표기 대상은 입력값을 `--tests` 필터로 **그대로** 전달한다(`.*`를 자동으로 덧붙이지 않는다). 호출 측이 완전한 필터를 지정한다: 패키지 전체는 `coffeeshout.foo.*`, 특정 클래스는 `coffeeshout.foo.BarTest`.

## Step 2: 에이전트 실행

에이전트 프롬프트는 다음과 같다:

```text
Working directory: 저장소 루트(backend 디렉터리)에서 실행한다

다음 명령을 실행하고 결과를 분석한다:
<Step 1 gradle 명령>

[성공]
✅ 모든 테스트 통과 (<대상>)

[실패]
1. build/test-results/**/*.xml 에서 <failure> 또는 <error> 태그를 포함한 파일만 찾는다
2. 다음 형식으로 보고한다:

❌ <N>개 테스트 실패 (<대상>)

- ClassName#methodName
  <오류 메시지 첫 줄>
```

**`--sync` 없음 (기본):** `run_in_background: true`로 실행 후 즉시 출력한다:

```text
🔄 테스트 실행 중... (<대상>)
완료되면 결과가 도착합니다.
```

**`--sync` 있음:** `run_in_background: false`로 실행하고 결과를 반환한다. 호출한 스킬이 결과를 받아 다음 단계를 결정한다.
