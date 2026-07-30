---
name: api-contract
description: BE 컨트랙트(WebSocket STOMP + HTTP/OpenAPI)를 api-mcp 서버로 조회·검증한다. 토픽/엔드포인트 목록, 페이로드 스키마, 발행 위치 확인, STOMP 연결 테스트, 요청 바디 스키마 대조에 사용한다.
---

# api-contract

BE 컨트랙트(WebSocket + HTTP)를 직접 `curl` 로 받아도 되지만, 본 레포는 `tools/api-mcp/` MCP 서버를 통해 Claude Code 에서 바로 조회한다. WebSocket 은 `GET /dev/ws-catalog`, HTTP 는 springdoc OpenAPI(`GET /v3/api-docs`)를 소비한다. (구 `ws-mcp` 에서 HTTP/OpenAPI 도구가 추가되며 `api-mcp` 로 통합됨)

| 도구 | 용도 |
| --- | --- |
| `ws_list_topics` | 전체 토픽/큐/send 목록 + path/description substring 검색 |
| `ws_describe` | 특정 path 의 풀 컨트랙트 (payloadType + publishers + 참조 schema) |
| `ws_source` | 특정 path 의 발행 메서드 위치 (className#methodName) |
| `ws_connect` / `ws_subscribe` / `ws_send` | STOMP 세션을 짧게 띄워 연결/구독/송신 검증 (`roomToken` 필요 — ADR-0009 참조) |
| `http_list_endpoints` | OpenAPI 엔드포인트 요약 목록 (method/path/summary) |
| `http_describe` | 특정 엔드포인트의 요청/응답 스키마 상세 |
| `http_request` | 실제 백엔드로 REST 요청 후 `{ request, response }` 반환 |
| `http_validate` | 바디를 OpenAPI 스키마와 대조한 누락/타입 불일치 리포트 |

**등록**: `frontend/.mcp.json` 에 이미 정의되어 있다(서버 키 `api`). `cd frontend && claude` 로 띄우면 자동 인식.

**MCP 빌드**: 별도 빌드 불필요. `frontend/.mcp.json` 이 self-healing 런처(`../tools/api-mcp/scripts/launch.mjs`)를 가리키므로 실행 시점에 의존성 설치·빌드를 자동 보장한다.

**컨트랙트 검증 위치**: api-mcp 의 zod 스키마와 BE 카탈로그의 일치(contract drift) 검증은 **BE CI(api-mcp CI)가 단독으로 소유**한다 — fixture 생성기(`WsCatalogFixtureGeneratorTest`, `-DupdateFixture=true`)·커밋된 fixture·BE 소스가 모두 `backend/` 에 있기 때문이다. `tools/api-mcp` 는 BE 소스의 미러이므로 FE CI 는 컨트랙트 검증을 수행하지 않고 빌드·린트·단위 테스트만 돌린다.

**prefix 주의사항**: MCP 카탈로그의 path 는 prefix 를 포함(`/topic/room/...`, `/user/queue/...`, `/app/...`)하지만, FE 의 `useWebSocketSubscription`/`send` 는 wrapper 가 prefix 를 자동 추가하므로 path 에서 `/topic`·`/app` 부분을 제거해 전달한다 (자세한 규칙은 `.claude/rules/websocket.md`).

상세 도구 명세·환경 변수·동작 검증(MCP Inspector) 은 `../tools/api-mcp/README.md` 참조.
