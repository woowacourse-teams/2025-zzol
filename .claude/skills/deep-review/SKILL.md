---
name: deep-review
description: 변경 diff를 여러 리뷰 렌즈(에이전트)로 병렬 검토하고, 발견사항을 신뢰도 채점으로 걸러 하나의 리포트로 병합한다. CodeRabbit 자동 리뷰 대체. 백엔드·프론트엔드 공통.
argument-hint: "[--base=브랜치명 (기본: dev)] [--comment (PR 코멘트로 게시)] [파일 경로...]"
allowed-tools: Read, Bash, Glob, Grep, Agent
---

# deep-review

CodeRabbit 제거(#1600)의 대체 수단이다. **메인 루프가 오케스트레이터**다 — 렌즈별 에이전트를 병렬로 띄우고, 결과를 채점·병합해 출력한다.

> **서브에이전트는 서브에이전트를 스폰할 수 없다.** 그래서 팬아웃·병합은 반드시 이 스킬(메인 루프)이 한다. 리뷰 에이전트를 오케스트레이터로 쓰려 하지 않는다.

## 1. 범위 판별

`$ARGUMENTS`에 파일 경로가 있으면 그 파일만, 없으면 브랜치 전체 diff를 대상으로 한다.

```bash
bash "$(git rev-parse --show-toplevel)/.claude/skills/deep-review/scope.sh" dev   # --base 값을 인자로
```

출력 플래그로 렌즈를 고른다.

| 플래그 | 의미 |
| --- | --- |
| `NO_CHANGE=1` | 리뷰 대상 없음 — 즉시 종료 |
| `SRC_EMPTY=1` | 문서·설정 변경만 — **모든 렌즈 건너뛰고** 그 사실을 보고 |
| `SRC_LINES=N` | 문서를 뺀 변경 줄 수. 렌즈 선택에는 안 쓰고, [issue-workflow](../../rules/issue-workflow.md)의 경량 경로 판정이 쓴다 |
| `SRC_BINARY=1` | 줄 수로 잴 수 없는 소스 변경(이미지·폰트 등)이 섞였다. 경량 경로 판정에서 전체로 본다 |
| `HAS_BE=1` / `HAS_FE=1` | 해당 스택 컨벤션 렌즈 실행 |
| `NEEDS_SECURITY=1` | 보안 렌즈 실행 |
| `DIRTY=1` | 커밋 안 된 변경은 범위 밖 — 사용자에게 알린다 |

`backend/.claude/*.md`를 `backend/` 변경으로 세면 Java 규칙으로 마크다운을 감수하는 헛수고가 된다. 그래서 스크립트가 `.claude/`·`docs/`·`*.md`·`.github/`를 걸러낸 뒤 스택을 판별한다.

리뷰 범위는 항상 `origin/$BASE...HEAD`(브랜치 전체)다. 에이전트 기본값은 마지막 커밋(`git diff HEAD~1`)이라 **프롬프트에 범위를 명시하지 않으면 어긋난다.**

대상 파일을 `src/main/java`·`frontend/src`로 **하드코딩하지 않는다** — `build.gradle`·설정·리소스 변경이 누락된다. 스택 경로(`-- backend/`·`-- frontend/`)로 스코프한 실제 diff 목록을 넘긴다.

## 2. 렌즈 팬아웃 (해당하는 렌즈를 한 응답에 전부 넣어 병렬 실행)

한 응답에 전부 넣어 병렬로 띄운다. **순차 호출하지 않는다.**

렌즈는 비동기로 돌고 메인 루프는 그동안 다른 일을 한다(CI 감시 등). 대신 **완료 알림이 와도 결과 본문은 안 오는 경우가 잦다**(2026-08-27 실행에서 7/7). 그러면 `SendMessage`로 다시 요청한다 — 요청문에 "파일에 쓰거나 요약하지 말고 본문 그대로"를 명시하고 빈손 선택지(발견사항 없음 / 미완료)를 준다. 3회까지 시도하고 끝내 못 받으면 리포트에 **"해당 렌즈 미회수"**를 남긴다 — 빈손을 "지적 없음"으로 보고하지 않는다.

| 렌즈 | `subagent_type` | 실행 조건 |
| --- | --- | --- |
| BE 컨벤션·ADR | `code-reviewer` | `HAS_BE=1` |
| FE 컨벤션·ADR·WS 컨트랙트 | `fe-code-reviewer` | `HAS_FE=1` |
| 범용 버그·정확성 | `bug-hunter` | 소스 변경 아무거나 |
| 과설계·삭제 후보 | `general-purpose` (ponytail 렌즈) | 소스 변경 아무거나 |
| 테스트 커버리지·컨벤션 | `test-verifier` | `HAS_BE=1` |
| 보안 | `general-purpose` (security 렌즈) | 아래 조건부 참조 |

`SRC_EMPTY=1`이면 위 표 전체를 건너뛴다.

스택 전용 에이전트를 섞지 않는다 — 백엔드는 `code-reviewer`, 프론트엔드는 `fe-code-reviewer`다. 백엔드 에이전트는 상대 경로(`docs/...`, `./gradlew`)를 쓰므로 프롬프트에 **`작업 기준 디렉터리: <REPO_ROOT>/backend`** 를 명시한다(프론트는 `<REPO_ROOT>/frontend`).

```text
Agent(subagent_type: "code-reviewer",
      prompt: "작업 기준 디렉터리: <REPO_ROOT>/backend. 리뷰 범위는 origin/$BASE...HEAD(브랜치 전체)다.
               대상 파일은 `git diff --name-only origin/$BASE...HEAD -- backend/` 결과 전체다(src/main/java 등으로 좁히지 말 것).
               발견사항만 텍스트로 반환한다.")

Agent(subagent_type: "fe-code-reviewer",
      prompt: "작업 기준 디렉터리: <REPO_ROOT>/frontend. 리뷰 범위는 origin/$BASE...HEAD(브랜치 전체)다.
               대상 파일은 `git diff --name-only origin/$BASE...HEAD -- frontend/` 결과 전체다(frontend/src 등으로 좁히지 말 것).
               발견사항만 텍스트로 반환한다.")

Agent(subagent_type: "bug-hunter",
      prompt: "리뷰 범위는 origin/$BASE...HEAD(브랜치 전체)다. 대상 파일: <diff 목록>. 발견사항만 텍스트로 반환한다.")

Agent(subagent_type: "test-verifier",
      prompt: "작업 기준 디렉터리: <REPO_ROOT>/backend. 리뷰 범위는 origin/$BASE...HEAD(브랜치 전체)다.
               대상은 `git diff --name-only origin/$BASE...HEAD -- backend/` 중 src/test/java 경로이며,
               변경된 src/main/java 코드에 대응 테스트가 없는 경우도 지적한다. 발견사항만 텍스트로 반환한다.")
```

### 과설계 렌즈 (ponytail)

`ponytail-review`는 스킬이라 에이전트에 위임한다. 서브에이전트에서 스킬 호출이 되는 것은 확인했다(2026-07-30, `general-purpose`에서 성공). 플러그인 미설치·미신뢰 환경을 위해 태그 기준 폴백을 함께 준다.

```text
Agent(subagent_type: "general-purpose",
      prompt: "`Skill(\"ponytail:ponytail-review\")` 를 호출해 origin/$BASE...HEAD diff를 과설계 관점으로 리뷰하라.
               스킬을 쓸 수 없으면 아래 태그 기준으로 직접 리뷰한다.
               - delete: 죽은 코드·안 쓰는 유연성·투기적 기능 → 대체물 없음
               - stdlib: 표준 라이브러리가 이미 주는 것을 손으로 구현 → 함수명 지목
               - native: 플랫폼·기존 의존성이 이미 하는 일 → 기능명 지목
               - yagni: 구현체 1개인 추상화, 아무도 안 바꾸는 설정, 호출자 1개인 계층
               - shrink: 같은 로직 더 짧게
               형식은 `<파일>:L<줄>: <태그> <무엇>. <대체>.` 한 줄씩. 발견사항만 반환한다.")
```

### 보안 렌즈 (조건부)

`NEEDS_SECURITY=1`일 때만 실행한다(스크립트가 인증·토큰·비밀값·필터 경로를 파일명으로 판별). 플래그가 없어도 diff 본문에 입력 검증·역직렬화·파일 업로드·SQL 직접 작성이 보이면 실행한다. 건너뛰면 그 사실을 리포트에 한 줄로 남긴다.

```text
Agent(subagent_type: "general-purpose",
      prompt: "`Skill(\"security-review\")` 를 호출해 origin/$BASE...HEAD 변경의 보안 취약점을 검토하라.
               스킬을 쓸 수 없으면 인증·인가 우회, 입력 검증 누락, 비밀값 노출, 인젝션, 권한 상승 관점으로 직접 검토한다.
               발견사항만 텍스트로 반환한다.")
```

## 3. 병합·중복 제거 (메인 루프)

렌즈 결과를 `파일:줄` 기준으로 합친다. 같은 지점을 여러 렌즈가 지적하면 **한 항목으로 합치고 근거를 나열**한다(예: "버그 + 컨벤션 위반"). 별도 병합 에이전트를 두지 않는다.

## 4. 신뢰도 채점 (80 컷)

CodeRabbit이 실제로 해주던 일은 노이즈 필터다. 병합 결과를 채점 에이전트 **1개**에 넘겨 항목별 0-100점을 받고 **80점 미만은 버린다**. 발견사항별 팬아웃은 하지 않는다(에이전트 수만 늘고 이득이 없다).

**ponytail 렌즈 결과는 이 채점에 넣지 않는다.** 과설계 지적은 "버그인가"를 묻는 루브릭에서 구조적으로 25점을 받아 전멸한다. 대신 별도 섹션으로 분리하고, **사실로 확인되는 것만**(사용처 0건, 구현체 1개, 중복 트리거처럼 세어서 검증 가능한 것) 남긴다. 취향 판단은 버린다.

```text
Agent(subagent_type: "general-purpose",
      prompt: "아래 코드 리뷰 발견사항이 진짜 문제인지 각각 0-100점으로 채점하라. 필요한 파일은 직접 읽어 검증한다.
               루브릭(그대로 적용):
               - 0: 가벼운 검증도 통과 못 하는 오탐, 또는 이번 변경 이전부터 있던 문제
               - 25: 진짜일 수도 있으나 검증하지 못했다. 컨벤션 문서가 명시하지 않은 스타일 지적
               - 50: 실재하지만 사소하거나 실제로 잘 발생하지 않는다
               - 75: 검증했고 실제로 발생할 가능성이 높다. 또는 컨벤션 문서가 직접 명시한 위반
               - 100: 확실하다. 근거가 직접 확인된다
               오탐으로 처리할 것: 이번 변경 이전부터 있던 문제 / 컴파일러·린터·타입체커·CI가 잡을 것
               / 시니어가 지적하지 않을 트집 / 의도된 것으로 보이는 동작 변경 / 사용자가 수정하지 않은 줄의 문제.
               빌드·타입체크는 직접 돌리지 않는다(CI가 한다).
               출력: 항목별 `점수 | 파일:줄 | 요약 | 채점 근거` 한 줄씩.
               발견사항:\n<병합 결과>")
```

## 5. 출력

`--comment`가 있으면 PR 코멘트 1개로 게시하고, 없으면 터미널에 리포트한다.

코멘트 문장은 [korean-style](../../rules/korean-style.md)을 따른다. 지적은 무엇이 문제인지부터 쓰고 근거를 뒤에 붙인다.

정돈 규칙: 상단에 심각도별 요약 → 파일별 발견사항 → 긴 개선 제안은 `<details>`로 접는다. 백엔드·프론트엔드 양쪽을 리뷰했으면 `### 백엔드`·`### 프론트엔드` 섹션으로 나눈다. 건너뛴 렌즈(보안 미해당 등)와 **80점 미만으로 버린 항목 개수**를 마지막 한 줄로 남긴다(무엇을 안 봤는지 감추지 않는다). 발견사항이 없으면 "리뷰 통과 — 지적사항 없음".

게시는 스크립트에 본문을 넘긴다. URL 조회·빈 값 가드·헤딩 부착을 스크립트가 처리한다 — 빈 URL로 코멘트가 엉뚱한 곳에 달리는 것을 막는다.

```bash
bash "$(git rev-parse --show-toplevel)/.claude/skills/deep-review/post-comment.sh" <<'EOF'
<정돈한 발견사항>
EOF
```

## 절대 규칙

리뷰 렌즈는 **제안만** 한다. 이 스킬은 프로덕션 코드를 수정하지 않는다. 수정은 사용자가 확인 후 `/fix`·`/impl`로 진행한다.
