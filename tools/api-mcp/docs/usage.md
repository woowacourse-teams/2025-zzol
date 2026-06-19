# api-mcp 사용 가이드

`api-mcp` 의 도구를 실제 개발 흐름에서 어떻게 쓰는지 정리한 문서다. 도구·환경변수 레퍼런스는 [README](../README.md)를 참조한다.

## 사전 준비

1. 빌드: `cd tools/api-mcp && npm install && npm run build`
2. 등록: `backend/.mcp.json` 이 `../tools/api-mcp/dist/server.js` 를 가리키는지 확인 (리포에 포함되어 있다).
3. 백엔드 실행: `cd backend && ./gradlew bootRun`
   - WebSocket 디스커버리(`ws_describe`/`ws_list_topics`/`ws_source`)는 캐시가 있으면 BE 없이도 동작한다.
   - HTTP 도구(`http_*`)와 STOMP 검증(`ws_connect`/`ws_subscribe`/`ws_send`)은 **BE 가 떠 있어야** 한다.
4. `cd backend && claude` 로 실행해야 `backend/.mcp.json` 이 인식된다.

## WebSocket 컨트랙트 흐름

"어떤 토픽이 있고, 페이로드가 뭐고, 누가 발행하는지" 를 코드 안 읽고 파악한다.

1. **토픽 찾기** — `ws_list_topics` (`kind`/`q` 로 필터). 예: 룸 관련 토픽만 보고 싶으면 `q: "room"`.
2. **컨트랙트 상세** — `ws_describe path:"/topic/room/{roomId}"`. 매칭된 topic/queue/send 의 풀 스키마와 참조 schema 를 반환한다.
3. **발행 위치** — `ws_source path:"..."`. 해당 컨트랙트를 발행/처리하는 `className#methodName` 을 알려준다.
4. **실제 검증** (BE 필요):
   - `ws_connect roomToken:"..."` 로 STOMP 연결 가능 여부 확인
   - `ws_subscribe topic:"..." roomToken:"..." durationMs:3000` 으로 실제 브로드캐스트 캡처
   - `ws_send destination:"..." roomToken:"..." payload:{...}` 로 송신 후 응답 확인

응답은 BE 의 `WebSocketResponse<T>` envelope(`{success, data, errorMessage, id}`) 그대로다.

## HTTP / OpenAPI 흐름

REST API 를 OpenAPI spec 기반으로 탐색·호출·검증한다. spec 은 `${API_MCP_BASE_URL}/v3/api-docs` (springdoc) 에서 받는다.

1. **엔드포인트 찾기** — `http_list_endpoints` (`method`/`tag`/`q` 로 필터). 예: `tag:"room"`, `method:"POST"`, `q:"join"`.
2. **스키마 상세** — `http_describe method:"POST" path:"/api/rooms"`. 요청/응답 스키마를 자세히 본다.
3. **실제 호출** (BE 필요) — `http_request`:

   ```text
   http_request path:"/api/rooms" method:"POST" body:{ "hostName": "..." }
   ```

   `{ request, response }` 형태로 반환한다. GET/HEAD/DELETE 등에 `body` 를 실으면 거부된다.
4. **바디 검증** — `http_validate method:"POST" path:"/api/rooms" requestBody:{...}`. 누락 필드·타입 불일치를 OpenAPI 스키마와 대조해 리포트한다. 응답 검증은 `responseBody`/`responseStatus` 를 함께 넘긴다.

## 자주 겪는 문제

| 증상                                     | 원인 / 해결                                                                       |
| ---------------------------------------- | --------------------------------------------------------------------------------- |
| HTTP 도구가 spec 로드 실패               | BE 가 안 떠 있거나 `API_MCP_BASE_URL` 이 잘못됨. `curl $BASE/v3/api-docs` 로 확인 |
| WS 디스커버리는 되는데 connect/send 실패 | STOMP 연결 대상(`API_MCP_BROKER_URL`) 확인. 기본은 catalog host + `/ws`           |
| 카탈로그가 옛날 내용                     | `API_MCP_CACHE_PATH`(기본 `~/.zzol-mcp/catalog.json`) 삭제 후 재요청              |
| `지원하지 않는 프로토콜` 에러            | `API_MCP_CATALOG_URL` 은 `http:`/`https:` 만 허용                                 |
| 도구가 Claude 에 안 뜸                   | `backend/` 에서 `claude` 를 실행했는지, `npm run build` 했는지 확인               |

## 컨트랙트 drift 감지

BE 의 WebSocket 카탈로그 시그니처가 바뀌면 `contract.test.ts` 가 fixture(`backend/app/src/test/resources/__fixtures__/ws-catalog.json`)를 zod 로 검증해 drift 를 잡는다. 자세한 갱신 절차는 [README 의 컨트랙트 fixture 절](../README.md#컨트랙트-fixture)을 참조한다.
