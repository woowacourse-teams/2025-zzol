---
description: code-reviewer 에이전트와 /code-review 스킬의 호출 방식 구분
---

## code-reviewer 에이전트 vs /code-review 스킬

### code-reviewer 에이전트 직접 호출

사용자가 다음과 같이 말하면 반드시 `Agent` 툴로 `.claude/agents/code-reviewer.md` 에이전트를 직접 호출한다:

- "code-reviewer 에이전트 호출"
- "코드 리뷰 에이전트 호출"
- "에이전트한테 코드 리뷰 시켜"
- "code-reviewer 에이전트"를 명시적으로 언급

```text
Agent(subagent_type: "code-reviewer", run_in_background: true, prompt: "...")
```

### /code-review 스킬 호출

사용자가 `/code-review`를 슬래시 커맨드로 직접 입력할 때만 `Skill("code-review")`를 사용한다.

"에이전트"라는 단어가 포함되면 무조건 Agent 툴 직접 호출이다. 스킬을 먼저 떠올리지 않는다.
