---
name: run-local
description: 현재 워크트리에서 백엔드·프론트엔드를 로컬로 띄운다. "서버 띄워줘", "로컬에서 실행해줘", 변경을 실제 앱에서 확인해야 할 때 사용한다. 워크트리마다 포트가 다르므로 동시에 여러 작업을 돌릴 수 있다.
---

# run-local

실제 셸은 [`run.sh`](run.sh)에 있다. 이 스킬은 **언제·어떻게 부를지**만 정한다 — 셸 명령을 여기 복사하지 않는다. 두 벌이 되면 갈라진다.

## 왜 스크립트를 따로 두나

막히는 지점 대부분은 Claude 전용이 아니다. 사람이 클론해도 똑같이 겪는다. 그래서 실행 로직은 저장소에 커밋된 스크립트에 두고, 이 스킬은 백그라운드 실행·로그 경로·포트 정리 같은 오케스트레이션만 맡는다.

## 1. 최초 1회 — 워크트리 준비

`.env`가 없거나 `.ports`가 없으면 먼저 돌린다. `create-issue`가 워크트리를 만들 때 이미 돌렸다면 건너뛴다.

```bash
bash "$(git rev-parse --show-toplevel)/.claude/skills/create-issue/worktree-setup.sh" "$(git rev-parse --show-toplevel)"
```

`.env` 계열을 주 저장소에서 심볼릭 링크하고, 이 워크트리 전용 포트 쌍을 `.ports`에 잡는다.

## 2. 실행

**항상 백그라운드로 띄우고 로그를 파일에 남긴다.** 포그라운드로 돌리면 세션이 묶여 아무 작업도 못 한다.

```bash
bash .claude/skills/run-local/run.sh backend   > <스크래치패드>/backend.log  2>&1 &
bash .claude/skills/run-local/run.sh frontend  > <스크래치패드>/frontend.log 2>&1 &
```

포트는 `.ports`에서 읽는다. 값이 필요하면 그 파일을 보면 된다(`BACKEND_PORT`·`FRONTEND_PORT`).

## 3. 기동 확인 — 로그가 아니라 엔드포인트로

컴파일 성공 메시지는 기동 완료가 아니다. 준비될 때까지 폴링한다.

```bash
curl -sf "http://localhost:$BACKEND_PORT/actuator/health"   # status: UP
curl -sf -o /dev/null "http://localhost:$FRONTEND_PORT/"
```

기동에 실패하면 로그에서 `Application run failed` 또는 `Caused by`를 찾는다.

## 4. 정리

포트를 잡은 채로 다시 띄우면 `EADDRINUSE`로 죽는다. 재시작 전에 끊는다.

```bash
lsof -nP -iTCP:"$BACKEND_PORT" -sTCP:LISTEN -t | xargs -r kill
```

`.ports`가 워크트리마다 다르므로, **다른 워크트리의 서버를 죽이지 않는다.** 여러 작업을 동시에 돌리는 것이 워크트리 분리의 목적이다.

## 알아둘 것

- **컨테이너는 자동으로 뜬다.** `bootRun`의 작업 디렉터리가 `backend/`라 `spring.docker.compose.file`이 해석된다. Docker가 꺼져 있으면 기동이 실패한다.
- **`.env`는 셸에서 소싱하지 않는다.** `springboot4-dotenv`가 `backend/.env`를 직접 읽는다. `source`는 값에 공백·따옴표가 있으면 깨진다.
- **프론트의 `API_URL`은 스크립트가 주입한다.** 심볼릭 링크된 `.env.development`는 모든 워크트리가 공유하므로 거기 값을 따르면 다른 워크트리의 백엔드에 붙는다.
- 로그인(OAuth)은 `backend/.env`의 실제 자격증명이 있어야 동작한다. 링크가 안 걸려 있으면 로그인만 조용히 실패한다.
