---
name: run-tests
description: 테스트를 에이전트에 위임해 실행하고 실패 분석 결과를 반환한다.
argument-hint: "[:module | package.pattern] [--sync]"
allowed-tools: Agent
---

# run-tests

테스트 실행을 에이전트에 위임한다. 기본은 백그라운드(비동기)이며, `--sync` 플래그를 붙이면 결과를 기다린다.

## 철칙

- 이 스킬의 유일한 동작은 **Agent 툴 호출 1회**다
- `Bash`로 `./gradlew`를 직접 실행하는 것은 **금지**다. 직접 실행하면 테스트가 끝날 때까지 메인 세션이 막혀 아무 작업도 할 수 없다
- `--sync`도 Bash 직접 실행이 아니다 — Agent를 `run_in_background: false`로 호출하는 것이다
- gradle 명령이 아무리 간단해 보여도 예외 없다. 실패 분석(XML 파싱)까지 에이전트 컨텍스트 안에서 끝내는 것이 목적이다

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
Working directory: backend 디렉터리(gradlew·settings.gradle.kts 위치)에서 실행한다 — git 루트는 그 상위이므로 동일하지 않다

다음 명령을 실행하고 결과를 분석한다:
<Step 1 gradle 명령>

[성공]
✅ 모든 테스트 통과 (<대상>)

[실패]
1. **/build/test-results/**/*.xml 에서 <failure> 또는 <error> 태그를 포함한 파일만 찾는다
   - 결과는 모듈별 <module>/build/test-results/ 에 있다. 1차 신호는 gradle 출력의 실패 요약으로 삼고
     XML은 상세 확인용으로 쓴다
   - 루트 backend/build/test-results/ 는 멀티모듈 전환 전 잔재일 수 있다(루트엔 test 소스가 없다). 신뢰하지 않는다
2. 다음 형식으로 보고한다:

❌ <N>개 테스트 실패 (<대상>)

- ClassName#methodName
  <오류 메시지 첫 줄>

3. XML이 하나도 없으면 컴파일·인프라 단계에서 깨진 것이다. gradle 출력 끝부분을 보고하고,
   Testcontainers/Docker 연결 오류면 "통합 테스트는 Docker 실행이 필요하다"를 함께 안내한다
```

**`--sync` 없음 (기본):** `run_in_background: true`로 실행 후 즉시 출력한다:

```text
🔄 테스트 실행 중... (<대상>)
완료되면 결과가 도착합니다.
```

**`--sync` 있음:** `run_in_background: false`로 실행하고 결과를 반환한다. 호출한 스킬이 결과를 받아 다음 단계를 결정한다.
