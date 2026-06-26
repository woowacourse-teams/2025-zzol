---
description: WebSocket 구독·발행 코드 작성 또는 수정 시 api-mcp 카탈로그로 BE 컨트랙트를 먼저 확인한다. destination prefix·payload·publisher 위치를 코드 작성 전에 검증해 prefix 중복·오타·존재하지 않는 토픽 구독을 사전 차단한다.
paths:
  - "src/apis/websocket/**"
  - "src/contexts/**"
  - "src/features/**/hooks/use*WebSocket*.ts"
  - "src/features/**/hooks/use*Subscription*.ts"
allowed-tools: Read, mcp__api__ws_list_topics, mcp__api__ws_describe, mcp__api__ws_source
---

# WebSocket 컨트랙트 조회 워크플로우

## 사전 검색 — 새 구독/발행 작성 전 필수

`useWebSocketSubscription` / `send` 호출을 새로 추가하거나 destination 을 수정할 때는 **반드시 api-mcp 의 `ws_*` 도구로 BE 카탈로그를 먼저 확인**한다. 존재하지 않는 토픽 구독, prefix 중복, payload 타입 오해를 사전에 차단한다.

```text
1. ws_list_topics q="<도메인 키워드>"   # 후보 토픽/큐 좁히기
2. ws_describe path=<선택한 path>      # 풀 컨트랙트 (payloadType + publishers + 참조 schema)
3. ws_source path=<...>                # 필요시 발행 메서드 위치 확인
```

`mcp__api__ws_*` 도구는 `frontend/.mcp.json` 으로 자동 등록된다. BE 가 안 떠 있어도 `~/.zzol-mcp/catalog.json` 캐시로 동작한다.

## destination 형식 — prefix 변환 규칙

api-mcp 카탈로그의 path 는 prefix 를 포함하지만, FE wrapper(`useWebSocketSubscription` / `send`)는 prefix 를 자동 추가한다. **path 에서 prefix 를 제거해 전달**한다.

| 카탈로그 path (MCP 응답) | FE 호출 시 path |
|---|---|
| `/topic/room/{joinCode}/winner` | `/room/{joinCode}/winner` |
| `/topic/room/{joinCode}/ladder/state` | `/room/{joinCode}/ladder/state` |
| `/app/room/{joinCode}/update-ready` (send destination) | `/room/{joinCode}/update-ready` |
| `/user/queue/friends/requests` (개인 큐) | `/queue/friends/requests` (`/user` 는 wrapper 가 자동) |

상세 룰은 `.claude/rules/websocket.md` 참조.

## 카탈로그에 없는 path 를 쓰려고 할 때

BE 측에 해당 토픽/큐가 아직 존재하지 않는다는 신호다. 임의로 새 destination 을 만들지 않는다. 두 선택지:

1. **기존 토픽 재사용**: `ws_list_topics q="..."` 로 유사 토픽 검색. 의미가 맞으면 그것을 쓴다.
2. **BE 측에 어노테이션 추가 요청**: 해당 컨트롤러/Publisher 에 `@WsTopic(path = "...", payload = <Type>.class)` 추가가 필요하다. PR 본문에 "BE: 토픽 신설 필요 (`/room/{joinCode}/...`, payload `XxxResponse`)" 명시하고 BE 측 PR 선행 후 fe/dev sync 후 진행한다.

## payload 타입 안전성

ws_describe 응답의 `payloadType` (예: `WebSocketResponse<List<PlayerResponse>>`) 과 `schemas` 의 필드 명세를 보고 onData 콜백의 타입을 정의한다. FE 의 wrapper 가 envelope 을 풀어 `data` 만 전달하는지 raw envelope 인지는 `useWebSocketSubscription` 시그니처를 확인한다 (보통 envelope 의 `data` 만 전달).

## ws_subscribe / ws_send 로 검증 (선택)

새 토픽 구독 로직을 짠 후 실제 메시지가 들어오는지 검증하려면:

```text
ws_subscribe topic=<full path> roomToken=<RST> durationMs=15000   # 캡처
ws_send destination=<full app path> roomToken=<RST> payload=...   # 트리거
```

`roomToken` 발급은 `POST /api/rooms/{joinCode}/session-token` (ADR-0009).
