## 공유 브랜치 보호 & push 안전

2026-06-11 작업 브랜치 push 중 `be/dev`가 PR 없이 직접 전진한 사고(#1404)의 재발방지 규칙이다. 원인은 작업 브랜치의 upstream(`branch.<name>.merge`)이 `be/dev`로 설정되어, 이후 push·IDE Sync가 작업 커밋을 `be/dev`로 직행시킨 것이다.

### 보호 브랜치 (직접 push·commit 금지)

`dev`, `prod`, `main`, `master`

> `dev`는 BE+FE 통합 브랜치다. **모든 작업(백엔드·프론트·풀스택)은 `dev`에서 분기해 `dev`로 PR한다.** 브랜치명은 prefix 없이 `{type}/{N}-{slug}`.
>
> `prod`는 BE+FE 통합 **프로덕션** 브랜치다(#1574). 승격은 `dev`→`prod` PR로만 하며, `prod` push가 곧 운영 배포 트리거다(backend-cd·frontend-cd).

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

### merge 권한

**Claude는 `gh pr merge`를 실행하지 않는다.** 보호 브랜치를 전진시키는 행위이고 되돌리기 어렵다. 리뷰 반영이 끝났다는 자체 판단만으로 진행하지 않고, 사용자가 명시적으로 merge를 지시했을 때만 실행한다.

지시받아 실행할 때는 squash로 합치고 커밋 제목을 PR 제목으로 고정한다 — `--subject`를 생략하면 GitHub이 커밋 목록을 합쳐 제목을 만들어 `[type] 설명` 컨벤션이 깨진다.

```bash
gh pr merge <번호> --squash --subject "[fix] 카드 점수 집계 누락 수정"
```

`prod` PR merge는 곧 운영 배포다. `dev`→`prod` 승격은 사용자 지시 없이 진행하지 않는다.

### push 한 커밋은 되쓰지 않는다 (force push 금지)

이미 origin에 올라간 커밋은 rebase·`reset --hard`·`commit --amend`로 갈아엎지 않는다. `push --force`(`--force-with-lease` 포함)를 쓰지 않는다.

**`dev`가 앞서가 충돌하면 `git rebase origin/dev`가 아니라 `git merge origin/dev`로 푼다.** PR은 squash로 합쳐지므로(위 merge 권한) 브랜치에 커밋이 몇 개든 merge 커밋이 섞이든 **어차피 하나가 된다** — rebase로 얻는 "깨끗한 히스토리"를 squash가 이미 보장한다.

되쓰면 잃는 것: ① 같은 저장소의 다른 워크트리·세션이 그 브랜치를 보고 있으면 꼬인다 ② force push 후 GitHub은 "지난 리뷰 이후 변경분"을 제대로 못 보여줘 리뷰어가 처음부터 다시 읽어야 한다. `--force-with-lease`는 남의 push만 막아줄 뿐 내 실수는 못 막는다.

이미 push한 커밋을 되돌려야 하면 `git revert`로 **새 커밋을 쌓는다**. 브랜치 범위가 잘못됐으면 되쓰지 말고 새 브랜치로 cherry-pick해 옮긴다.

#### `git pull`은 브랜치가 push됐는지로 갈린다

`--rebase`는 로컬 커밋을 다시 쓴다. 그 커밋이 이미 push된 것이면 force push 없이는 다시 올릴 수 없게 되고, 결국 위 금지를 어길 수밖에 없어진다.

```bash
# 이 브랜치가 origin에 이미 있나?
git ls-remote --exit-code --heads origin "$(git branch --show-current)" >/dev/null
```

- **있으면(push 이력 O)** → `git pull --no-rebase`. `dev` 동기화도 `git merge origin/dev`.
- **없으면(push 이력 X)** → `git pull --rebase` 사용 가능.

판별이 번거로우면 그냥 `--no-rebase`를 쓴다. squash가 히스토리를 정리하므로 잃는 게 없다.

### 이 규칙의 한계 (사용자 인지 필요)

이 규칙은 **Claude의 git 조작에만** 적용된다. 사용자의 수동 push나 VS Code "Push/Sync" 버튼은 이 규칙으로 막지 못한다. 다만 Claude가 작업 브랜치 upstream을 보호 브랜치로 만들지 않으면, IDE Sync가 보호 브랜치를 target으로 삼을 소지 자체가 사라진다(근본 원인 제거). 완전 차단이 필요하면 서버측 `enforce_admins=true` 또는 로컬 pre-push hook을 별도로 도입한다.
