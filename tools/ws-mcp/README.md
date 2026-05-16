# ws-mcp

zzol WebSocket 컨트랙트 디스커버리용 MCP 서버. `GET /dev/ws-catalog` 응답을 소비해 토픽/큐/send 메타데이터를 Claude / FE 개발자에게 노출하고, STOMP 세션을 짧게 띄워 연결·구독·송신 검증을 도와줍니다.

## 도구 (MVP, 6종)

| 도구 | 입력 | 출력 |
|---|---|---|
| `ws_describe` | `path` | 매칭된 topic/queue/send 의 풀 컨트랙트 + 참조 schema |
| `ws_list_topics` | `kind?`, `q?` | topics/queues/sends 요약 목록 |
| `ws_source` | `path` | 발행/처리 메서드 위치 (`className#methodName`) |
| `ws_connect` | `roomToken`, `joinCode?`, `playerName?` | STOMP 연결 가능 여부 |
| `ws_subscribe` | `topic`, `roomToken`, `durationMs?` | envelope raw 메시지 캡처 배열 |
| `ws_send` | `destination`, `roomToken`, `payload?`, `waitForResponseTopic?` | 송신 결과 + 후속 응답 |

응답 메시지는 BE 의 `WebSocketResponse<T>` envelope 을 그대로 (`{success, data, errorMessage, id}`) 반환합니다.

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `WS_MCP_CATALOG_URL` | `http://localhost:8080/dev/ws-catalog` | BE 카탈로그 엔드포인트 |
| `WS_MCP_BROKER_URL` | catalog URL 의 host + `/ws` | STOMP brokerURL (예: `ws://localhost:8080/ws`) |

카탈로그를 한 번 받아 `~/.zzol-mcp/catalog.json` 에 캐싱합니다. BE 가 안 켜져 있어도 캐시가 있으면 디스커버리 도구는 동작합니다.

## 등록 — 모노레포 루트 `.mcp.json`

```json
{
  "mcpServers": {
    "ws": {
      "command": "node",
      "args": ["./tools/ws-mcp/dist/server.js"],
      "env": {
        "WS_MCP_CATALOG_URL": "http://localhost:8080/dev/ws-catalog"
      }
    }
  }
}
```

Claude Code 가 자동 인식합니다. `npm run build` 후 사용.

## 개발

```bash
cd tools/ws-mcp
npm install
npm test              # L1 단위 + L2 컨트랙트 (BE fixture vs zod)
npm run typecheck
npm run build
```

## 동작 확인 (MCP Inspector)

```bash
# 1. BE 띄우기
cd ../../backend && ./gradlew bootRun

# 2. 카탈로그 응답 확인
curl localhost:8080/dev/ws-catalog | jq .

# 3. Inspector 로 GUI 호출
npx @modelcontextprotocol/inspector node ./tools/ws-mcp/dist/server.js
```

## 컨트랙트 fixture

BE 의 `WsCatalogFixtureExportTest` 가 실제 `/dev/ws-catalog` 응답을 `backend/src/test/resources/__fixtures__/ws-catalog.json` 으로 저장합니다. MCP 의 `contract.test.ts` 가 이 파일을 zod 로 검증해 BE ↔ MCP drift 를 감지합니다.

BE 카탈로그 시그니처를 바꿨다면 BE 테스트 한 번 돌려 fixture 를 갱신하고 commit 하세요:

```bash
cd backend && ./gradlew test --tests WsCatalogFixtureExportTest
git status __fixtures__/ws-catalog.json
git add backend/src/test/resources/__fixtures__/ws-catalog.json
```

fixture 가 갱신되면 GitHub Actions 의 `ws-mcp-ci` 워크플로우가 자동 트리거되어 MCP 의 zod 스키마와 일치 여부를 검증합니다.
