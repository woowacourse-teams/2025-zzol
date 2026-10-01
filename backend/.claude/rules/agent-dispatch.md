---
description: 리뷰 요청을 code-reviewer 에이전트 단독 호출과 deep-review 스킬 중 무엇으로 처리할지 구분
---

## code-reviewer 에이전트 vs deep-review 스킬

### code-reviewer 에이전트 직접 호출

사용자가 다음과 같이 말하면 반드시 `Agent` 툴로 `.claude/agents/code-reviewer.md` 에이전트를 직접 호출한다:

- "code-reviewer 에이전트 호출"
- "코드 리뷰 에이전트 호출"
- "에이전트한테 코드 리뷰 시켜"
- "code-reviewer 에이전트"를 명시적으로 언급

```text
Agent(subagent_type: "code-reviewer", run_in_background: true, prompt: "...")
```

"에이전트"라는 단어가 포함되면 무조건 Agent 툴 직접 호출이다. 스킬을 먼저 떠올리지 않는다.

### deep-review 스킬 호출

여러 렌즈(컨벤션·버그·과설계·테스트·보안)를 병렬로 돌려 종합 리뷰가 필요할 때만 `Skill("deep-review")`를 쓴다. 사용자가 `/deep-review`를 직접 입력한 경우, 또는 `create-pr`의 리뷰 단계가 그렇다.

`code-reviewer` 단독 호출은 **백엔드 컨벤션·계층·ADR 렌즈 하나만** 도는 것이고, `deep-review`는 그 렌즈를 포함한 전체 팬아웃이다.

> 내장 `/code-review`는 사용자 입력 전용 커맨드다. `Skill("code-review")`로 호출할 수 없다(메인 루프·서브에이전트 모두). 스킬·에이전트 정의에 넣지 않는다.
