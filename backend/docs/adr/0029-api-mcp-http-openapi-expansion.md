# 0029. api-mcp — WebSocket 디스커버리에서 HTTP/OpenAPI 디스커버리로 확장 + `ws-mcp` 리네임

- 날짜: 2026-06-15
- 상태: 적용됨 (PR #1289 머지)

## 컨텍스트

ADR-0012 로 `ws-mcp` MCP 서버를 도입해 WebSocket 컨트랙트(토픽/큐/send)를 Claude·FE 개발자에게 노출했다. 운영해 보니 REST API 에도 동일한 통증이 그대로 있었다.

- **FE/BE 분리 워크플로우의 컨트랙트 단절**: `fe/dev`·`be/dev` 가 분리돼 있어 FE 는 어떤 REST 엔드포인트가 있고 요청/응답 스키마가 무엇인지 코드를 직접 읽어야 알 수 있다. (ADR-0012 가 WebSocket 에 대해 풀려던 문제와 같다.)
- **springdoc 자산 미활용**: 백엔드는 이미 springdoc 으로 `GET /v3/api-docs` OpenAPI spec 을 제공하지만, 이를 소비해 탐색·호출·검증하는 흐름이 없었다.
- **이름과 범위의 불일치**: 도구가 `ws_*` 만 있고 서버 이름이 `ws-mcp` 라 HTTP 도구를 담기에 의미가 맞지 않았다.

## 결정

1. **리네임 `ws-mcp` → `api-mcp`**: 디렉터리(`tools/ws-mcp` → `tools/api-mcp`), 패키지명(`@zzol/api-mcp`), MCP 서버 이름(`api-mcp`), CI 워크플로(`api-mcp-ci.yml`/job `api-mcp`), `backend/.mcp.json` 서버 키(`ws` → `api`), dependabot 디렉터리/라벨까지 일괄 정리한다.
2. **HTTP/OpenAPI 도구 4종 추가** — springdoc `GET /v3/api-docs` 를 단일 소스로 소비한다.
   - `http_list_endpoints` — `method`/`tag`/`q` 필터로 엔드포인트 요약 목록
   - `http_describe` — 특정 엔드포인트의 요청/응답 스키마 상세
   - `http_request` — 실제 백엔드로 요청 후 `{ request, response }` 반환 (body 금지 메서드 가드 포함)
   - `http_validate` — 요청/응답 바디를 OpenAPI 스키마와 대조한 누락/타입 불일치 리포트
3. **환경 변수 접두사 `API_MCP_*` 로 통일**: 구 `WS_MCP_CATALOG_URL`/`WS_MCP_BROKER_URL`/`WS_MCP_CACHE_PATH`/`HTTP_MCP_BASE_URL` 을 `API_MCP_CATALOG_URL`/`API_MCP_BASE_URL`/`API_MCP_BROKER_URL`/`API_MCP_CACHE_PATH` 로 바꾼다.

## 대안

- **별도 `http-mcp` 서버 신설**: WS 와 HTTP 를 독립 서버로 둔다. → `.mcp.json` 에 서버 2개를 등록해야 하고, catalog URL 에서 base/broker URL 을 파생하는 공통 로직과 캐시 인프라를 중복 운영해야 한다. 한 백엔드의 두 컨트랙트일 뿐이라 단일 서버가 응집도가 높다. **기각**.
- **ADR-0012 개정으로 흡수**: → 0012 는 백엔드 측 어노테이션(`@WsTopic` 등)과 `/dev/ws-catalog` 생성에 관한 결정이고, 본 결정은 MCP(클라이언트) 측 도구 확장이라 관심사가 다르다. 별도 ADR 로 분리하고 0012 를 참조한다.
- **env 접두사 유지(`WS_MCP_*` + `HTTP_MCP_BASE_URL` 혼용)**: breaking change 를 피할 수 있으나 도구명(api)과 env(ws/http) 불일치가 영구화된다. 내부 개발 도구이고 `.mcp.json` 을 같은 PR 에서 갱신하므로 통일 비용이 작다고 판단해 **통일**을 택한다.

## 결과 / 제약

- **오프라인 동작 차이**: `ws_*` 디스커버리는 카탈로그 캐시가 있으면 BE 없이도 동작한다. `http_*` 와 STOMP 검증(`ws_connect`/`ws_subscribe`/`ws_send`)은 실제 BE 가 필요하다.
- **breaking — env 접두사 변경**: 셸에 `WS_MCP_*` 를 직접 export 해 쓰던 경우 `API_MCP_*` 로 갱신해야 한다. 리포의 `backend/.mcp.json` 은 같은 PR 에서 갱신했다.
- **보안/범위**: `http_request` 는 실제 HTTP 요청을 보낸다. 로컬/개발 서버를 대상으로 하는 **개발 도구** 전제이며, 운영 서버 대상 사용은 의도하지 않는다.
- **컨트랙트 drift**: 기존대로 `contract.test.ts` 가 `backend/app/src/test/resources/__fixtures__/ws-catalog.json` 를 zod 로 검증한다. HTTP 쪽은 OpenAPI spec 을 런타임에 받으므로 별도 fixture 가 없다.

## 참고

- ADR-0012 — WebSocket 컨트랙트 디스커버리 (`@WsTopic`/`@WsQueue`/`@WsReceive` + `/dev/ws-catalog`)
- `tools/api-mcp/README.md`, `tools/api-mcp/docs/usage.md`
