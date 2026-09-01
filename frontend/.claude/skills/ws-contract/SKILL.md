---
description: WebSocket 구독·발행 코드 작성 또는 수정 시 BE 가 생성한 ws-catalog fixture 로 컨트랙트를 먼저 확인한다. destination prefix·payload·publisher 위치를 코드 작성 전에 확인해 prefix 중복·오타·존재하지 않는 토픽 구독을 막는다.
paths:
  - "src/apis/websocket/**"
  - "src/contexts/**"
  - "src/features/**/hooks/use*WebSocket*.ts"
  - "src/features/**/hooks/use*Subscription*.ts"
allowed-tools: Read, Grep
---

# WebSocket 컨트랙트 조회 워크플로우

## 컨트랙트는 어디에 있나

`backend/app/src/test/resources/__fixtures__/ws-catalog.json` 이 SSOT 다. BE 의 `@WsTopic`·`@WsQueue`·`@WsReceive` 애노테이션에서 생성되며, 소스보다 낡으면 backend-ci 가 실패한다(ADR-0037).

원본 애노테이션을 직접 보고 싶으면 그쪽이 더 정확하다.

```bash
grep -rn '@WsTopic(path = "/room' backend/
```

## 사전 확인 — 새 구독·발행 작성 전

`useWebSocketSubscription` / `useUserSocketSubscription` / `send` 호출을 새로 추가하거나 destination 을 수정할 때는 fixture 에서 그 경로를 먼저 찾는다. 없는 경로를 쓰면 STOMP 는 구독에 성공하고 publish 는 조용히 버려져서 런타임에 아무 신호도 안 온다.

```bash
# 후보 좁히기
jq -r '.topics[].path, .queues[].path' backend/app/src/test/resources/__fixtures__/ws-catalog.json | grep worm

# 풀 컨트랙트 (payloadType + publishers + 참조 schema)
jq '.topics[] | select(.path == "/topic/room/{joinCode}/worm/state")' backend/app/src/test/resources/__fixtures__/ws-catalog.json
```

## destination 형식 — prefix 변환 규칙

FE wrapper 가 prefix 를 붙이므로 **카탈로그 path 에서 붙는 만큼을 빼고 전달**한다. 규칙은 `useWebSocketMessaging.ts` 두 줄이 전부다. 구독은 broker destination(`/user/`·`/queue/`)이 아닐 때만 `/topic` 을 붙이고, 발행은 무조건 `/app` 을 붙인다.

| 카탈로그 path                                          | FE 호출 시 path                    |
| ------------------------------------------------------ | ---------------------------------- |
| `/topic/room/{joinCode}/winner`                        | `/room/{joinCode}/winner`          |
| `/topic/room/{joinCode}/ladder/state`                  | `/room/{joinCode}/ladder/state`    |
| `/app/room/{joinCode}/update-ready` (send destination) | `/room/{joinCode}/update-ready`    |
| `/user/queue/friends/requests` (개인 큐)               | `/user/queue/friends/requests` (그대로) |

개인 큐는 wrapper 가 아무것도 붙이지 않으므로 `/user/` 를 포함해 그대로 넘긴다. 상세 룰은 `.claude/rules/websocket.md` 를 참조한다.

## 카탈로그에 없는 path 를 쓰려고 할 때

BE 에 그 토픽이나 큐가 아직 없다는 뜻이다. 임의로 새 destination 을 만들지 않는다. 만들어도 `wsContract` 테스트가 CI 에서 잡는다.

1. **기존 토픽 재사용**: fixture 에서 유사 토픽을 찾아 의미가 맞으면 그것을 쓴다.
2. **BE 에 애노테이션 추가 요청**: 해당 Publisher 나 컨트롤러에 `@WsTopic(path = "...", payload = <Type>.class)` 가 필요하다. PR 본문에 "BE: 토픽 신설 필요 (`/room/{joinCode}/...`, payload `XxxResponse`)" 를 적고 BE PR 을 선행한다.

## payload 타입

fixture 의 `payloadType`(예: `WebSocketResponse<List<PlayerResponse>>`)과 `schemas` 의 필드 명세를 보고 onData 콜백 타입을 정의한다.

envelope 을 푸는지는 소켓마다 다르다. 방 소켓(`useWebSocketSubscription`)은 wrapper 가 envelope 을 풀어 `data` 만 넘기고, 유저 소켓(`useUserSocketSubscription`)은 envelope 을 통째로 넘긴다.

## 검증

```bash
cd frontend && npm run test:jest -- wsContract
```

구독·발행 destination 이 fixture 에 있는지 대조한다. 실패하면 어느 파일 몇 번째 줄의 어떤 경로가 문제인지 함께 나온다. 정적으로 판정할 수 없는 destination 도 실패로 잡히며, 의도한 예외는 `// ws-contract-ignore` 주석으로만 넘긴다.
