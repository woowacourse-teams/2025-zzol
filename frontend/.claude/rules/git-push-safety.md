## 공유 브랜치 보호 & push 안전

2026-06-11 작업 브랜치 push 중 `be/dev`가 PR 없이 직접 전진한 사고(#1404)에서 정립된 push 안전 규칙의 프론트엔드 적용판이다. 원인은 작업 브랜치의 upstream(`branch.<name>.merge`)이 보호 브랜치로 설정되어, 이후 push·IDE Sync가 작업 커밋을 보호 브랜치로 직행시킨 것이다. FE 작업은 `fe/*` 브랜치에서 이뤄지므로 동일 사고가 `fe/dev`에서도 재현될 수 있다.

### 보호 브랜치 (직접 push·commit 금지)

`fe/dev`, `fe/prod`, `main`, `be/dev`, `be/prod`, `master`

이 브랜치들의 변경은 **PR로만** 반영한다. Claude는 어떤 경우에도 이 브랜치로 직접 push하거나, 이 브랜치를 체크아웃해 직접 커밋하지 않는다. FE 작업의 기본 머지 대상은 `fe/dev`이며, 릴리스는 `fe/prod`다.

### 작업 브랜치 upstream을 보호 브랜치로 두지 않기 (★ 사고 핵심 원인)

- `fe/dev` 등에서 체크아웃하거나 `git worktree add ... origin/fe/dev`로 만들면 git의 `autoSetupMerge` 기본동작이 upstream을 `fe/dev`로 잡는다. **반드시 떼어낸다**: `git branch --unset-upstream`.
- 금지 패턴:

```bash
# 금지 — upstream이 fe/dev가 되어 이후 push가 fe/dev로 직행
git checkout -b fe/feat/x fe/dev && git branch -u origin/fe/dev
git push -u origin fe/feat/x:fe/dev
```

- 첫 push는 반드시 **자기 이름**으로 하고 upstream도 자기 이름으로 잡는다:

```bash
git push -u origin HEAD:fe/feat/x
```

### push 전 검증 절차

1. `git rev-parse --abbrev-ref @{u}` 로 현재 upstream을 확인한다. 결과가 보호 브랜치면 **중단하고 사용자에게 보고**한다.
2. push는 인자 없는 bare `git push` 대신 **명시 refspec**을 사용한다: `git push origin HEAD:<work-branch>`.
3. push 명령의 destination에 보호 브랜치명이 나타나면 실행하지 않고 사용자에게 보고한다.

### 이 규칙의 한계 (사용자 인지 필요)

이 규칙은 **Claude의 git 조작에만** 적용된다. 사용자의 수동 push나 VS Code "Push/Sync" 버튼은 이 규칙으로 막지 못한다. 다만 Claude가 작업 브랜치 upstream을 보호 브랜치로 만들지 않으면, IDE Sync가 보호 브랜치를 target으로 삼을 소지 자체가 사라진다(근본 원인 제거). 완전 차단이 필요하면 서버측 `enforce_admins=true` 또는 로컬 pre-push hook을 별도로 도입한다.
