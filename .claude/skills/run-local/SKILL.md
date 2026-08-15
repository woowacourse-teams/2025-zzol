---
name: run-local
description: 현재 워크트리에서 백엔드·프론트엔드를 로컬로 띄운다. "서버 띄워줘", "로컬에서 실행해줘", 변경을 실제 앱에서 확인해야 할 때 사용한다.
---

# run-local

실제 셸은 [`run.sh`](run.sh)에 있다. 이 스킬은 **언제·어떻게 부를지**만 정한다 — 셸 명령을 여기 복사하지 않는다. 두 벌이 되면 갈라진다.

## 한 번에 한 워크트리만 띄운다

포트는 백엔드 `8080`·프론트 `3000` **고정**이다. 워크트리마다 포트를 달리하는 방안을 시도했으나 성립하지 않았다 — `application-local.yml`의 `web.cors.allowed-origins`·`user.oauth.frontend-redirect-uri`·`room.qr.prefix`와 `docker-compose.yml`의 호스트 포트가 전부 그 두 값으로 하드코딩돼 있다. 포트만 바꾸면 **화면은 뜨는데 API가 전부 CORS로 막히는** 형태로 반쯤 깨진다.

그래서 포트가 잡혀 있으면 다른 포트로 새지 않고 `run.sh`가 **멈춘다.** 다른 워크트리에서 띄운 것을 먼저 끄면 된다.

> 여러 워크트리를 정말 동시에 띄워야 하면 위 하드코딩부터 걷어내야 한다 — 별도 이슈감이다.

## 1. 최초 1회 — 워크트리 준비

`.env`가 없으면 먼저 돌린다. `create-issue`가 워크트리를 만들 때 이미 돌렸다면 건너뛴다.

**워크트리 안에서만 돌린다.** 주 저장소를 대상으로 주면 스크립트가 `ABORT`로 막는다 — 자기 자신을 가리키는 링크가 되어 원본 `.env`가 파괴되기 때문이다.

```bash
# 워크트리 루트에서
WT="$(git rev-parse --show-toplevel)"
MAIN="$(git worktree list --porcelain | sed -n '1s/^worktree //p')"
bash "$MAIN/.claude/skills/create-issue/worktree-setup.sh" "$WT"
```

## 2. 실행

**항상 백그라운드로 띄우고 로그를 파일에 남긴다.** 포그라운드로 돌리면 세션이 묶여 아무 작업도 못 한다.

```bash
bash .claude/skills/run-local/run.sh backend   > <스크래치패드>/backend.log  2>&1 &
bash .claude/skills/run-local/run.sh frontend  > <스크래치패드>/frontend.log 2>&1 &
```

## 3. 기동 확인 — 로그가 아니라 엔드포인트로

컴파일 성공 메시지는 기동 완료가 아니다. 준비될 때까지 폴링한다.

```bash
curl -sf http://localhost:8080/actuator/health   # status: UP
curl -sf -o /dev/null http://localhost:3000/
```

기동에 실패하면 로그에서 `Application run failed` 또는 `Caused by`를 찾는다.

**화면이 뜬 것과 동작하는 것은 다르다.** API가 실제로 오가는지 봐야 하면 프론트 origin으로 요청을 보내 확인한다.

```bash
curl -s -o /dev/null -w '%{http_code}\n' -H 'Origin: http://localhost:3000' \
  -X OPTIONS -H 'Access-Control-Request-Method: GET' http://localhost:8080/users/me/friends
```

## 4. 정리

포트를 잡은 채 다시 띄우면 `run.sh`가 멈춘다. 재시작 전에 끊는다.

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN -t | xargs -r kill
```

## 알아둘 것

- **컨테이너는 자동으로 뜬다.** `bootRun`의 작업 디렉터리가 `backend/`라 `spring.docker.compose.file`이 해석된다. Docker가 꺼져 있으면 기동이 실패한다.
- **MySQL·Valkey는 워크트리 간 공유된다.** compose 프로젝트명이 파일 부모 디렉터리명(`backend`)이라 모든 워크트리가 같은 컨테이너를 쓴다. `ddl-auto: create`이므로 **백엔드를 띄울 때마다 스키마가 새로 만들어진다** — 다른 워크트리에서 쌓아둔 로컬 데이터는 남지 않는다.
- **`.env`는 셸에서 소싱하지 않는다.** `springboot4-dotenv`가 `backend/.env`를 직접 읽는다. `source`는 값에 공백·따옴표가 있으면 깨진다.
- 로그인(OAuth)은 `backend/.env`의 실제 자격증명이 있어야 동작한다. 링크가 안 걸려 있으면 로그인만 조용히 실패한다.
