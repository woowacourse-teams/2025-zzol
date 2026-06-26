## 공유 브랜치 보호 & push 안전

2026-06-11 작업 브랜치 push 중 `be/dev`가 PR 없이 직접 전진한 사고(#1404)의 재발방지 규칙이다. 원인은 작업 브랜치의 upstream(`branch.<name>.merge`)이 `be/dev`로 설정되어, 이후 push·IDE Sync가 작업 커밋을 `be/dev`로 직행시킨 것이다.

### 보호 브랜치 (직접 push·commit 금지)

`dev`, `be/dev`, `be/prod`, `fe/dev`, `fe/prod`, `main`, `master`

> `dev`는 BE+FE 통합 브랜치다. **모든 작업(백엔드·프론트·풀스택)은 `dev`에서 분기해 `dev`로 PR한다.** 브랜치명은 prefix 없이 `{type}/{N}-{slug}`.
>
> `be/dev`·`fe/dev`·`be/prod`·`fe/prod`는 전환기 호환으로 보호 목록에 남기지만 **신규 분기·PR 대상이 아니다**. 원격에서 이 브랜치들을 삭제하면 이 목록과 `backend/.claude/skills/commit/preflight.sh`의 `PROTECTED`에서도 제거한다(TODO).

이 브랜치들의 변경은 **PR로만** 반영한다. Claude는 어떤 경우에도 이 브랜치로 직접 push하거나, 이 브랜치를 체크아웃해 직접 커밋하지 않는다.

### 작업 브랜치 upstream을 보호 브랜치로 두지 않기 (★ 사고 핵심 원인)

- `dev` 등에서 체크아웃하거나 `git worktree add ... origin/dev`로 만들면 git의 `autoSetupMerge` 기본동작이 upstream을 `dev`로 잡는다. **반드시 떼어낸다**: `git branch --unset-upstream`.
- 금지 패턴:

```bash
# 금지 — upstream이 dev가 되어 이후 push가 dev로 직행
git checkout -b feat/x dev && git branch -u origin/dev
git push -u origin feat/x:dev
```

- 첫 push는 반드시 **자기 이름**으로 하고 upstream도 자기 이름으로 잡는다:

```bash
git push -u origin HEAD:{type}/{N}-{slug}
```

### push 전 검증 절차

1. `git rev-parse --abbrev-ref @{u}` 로 현재 upstream을 확인한다. 결과가 보호 브랜치면 **중단하고 사용자에게 보고**한다.
2. push는 인자 없는 bare `git push` 대신 **명시 refspec**을 사용한다: `git push origin HEAD:<work-branch>`.
3. push 명령의 destination에 보호 브랜치명이 나타나면 실행하지 않고 사용자에게 보고한다.

### 이 규칙의 한계 (사용자 인지 필요)

이 규칙은 **Claude의 git 조작에만** 적용된다. 사용자의 수동 push나 VS Code "Push/Sync" 버튼은 이 규칙으로 막지 못한다. 다만 Claude가 작업 브랜치 upstream을 보호 브랜치로 만들지 않으면, IDE Sync가 보호 브랜치를 target으로 삼을 소지 자체가 사라진다(근본 원인 제거). 완전 차단이 필요하면 서버측 `enforce_admins=true` 또는 로컬 pre-push hook을 별도로 도입한다.
