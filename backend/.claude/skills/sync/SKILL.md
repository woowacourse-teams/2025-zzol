---
name: sync
description: be/dev 최신 변경사항을 현재 브랜치에 rebase한다. 충돌 발생 시 파일별로 안내한다.
argument-hint: "[--branch=브랜치명 (기본: be/dev)]"
allowed-tools: Bash
---

# sync

현재 브랜치를 `be/dev`(또는 지정 브랜치)로 rebase해 최신 상태로 유지한다.

## Step 1: 현재 상태 확인

```bash
git status --short
git branch --show-current
```

**언스테이지 변경사항이 있으면** rebase 전에 사용자에게 선택지를 제공한다:
- stash 후 진행 (`git stash`)
- 커밋 후 진행 (`/commit` 사용 권장)
- 중단

## Step 2: 원격 최신화

```bash
git fetch origin
```

`$ARGUMENTS`에서 `--branch=` 값을 파싱한다. 없으면 `be/dev` 사용.

## Step 3: 차이 확인

```bash
git log HEAD..origin/be/dev --oneline
```

이미 최신 상태면 "이미 be/dev와 동기화되어 있습니다"를 출력하고 종료한다.

## Step 4: Rebase 실행

```bash
git rebase origin/be/dev
```

**충돌 발생 시:**
1. `git status`로 충돌 파일 목록을 확인한다
2. 각 파일에 대해 충돌 내용을 읽고 해결 방향을 사용자에게 설명한다
3. 사용자 확인 후 충돌을 해결한다
4. `git add <resolved-files>` → `git rebase --continue`
5. 추가 충돌이 있으면 반복한다

rebase 중단이 필요하면: `git rebase --abort`

## Step 5: 완료 보고

```bash
git log --oneline -5
```

rebase 결과 요약을 출력한다:
- 가져온 커밋 수
- 현재 브랜치의 HEAD 커밋
