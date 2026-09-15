# 0037. 조회형 api-mcp 를 폐기하고 FE↔BE WebSocket 계약을 생성 타입과 tsc 로 강제

- 날짜: 2026-09-02
- 상태: 적용됨 (이슈 #1733)

## 컨텍스트

ADR-0012 가 `@WsTopic`·`@WsQueue`·`@WsReceive` 애노테이션과 `GET /dev/ws-catalog` 로 WebSocket 계약을 기계가 읽을 수 있게 만들었고, ADR-0029 가 그 소비자인 MCP 서버에 HTTP 도구를 더했다. 두 ADR 의 동기는 같다. `fe/dev` 와 `be/dev` 가 분리돼 있어 FE 작업 중에 BE 코드를 읽을 수 없다는 것이다.

그 전제가 사라졌고, 남은 도구는 쓰이지 않았다.

- **전제 소멸**: 커밋 `64377832` 로 `dev` 통합 브랜치가 생겨 FE 작업 중에도 BE 소스를 그대로 읽는다.
- **실사용 0건**: 전체 세션 기록에 `mcp__api__*` 도구 호출이 없다. 캐시 디렉터리 `~/.zzol-mcp/` 는 생성된 적조차 없다.
- **유지 비용은 실재**: 소스 1,731줄, 의존성 14개, 전용 CI 워크플로 1개. 기능 개발은 2026-06-20 에 멈췄고 dependabot PR 만 12건 쌓였다.

계약이 어긋난 코드도 배포돼 있었다. `RacingGamePlayPage.tsx` 가 `racing-game/start` 로 보내는데 BE 에는 `racing-game/tap` 핸들러뿐이었다. STOMP 는 없는 토픽 구독에도 성공하고 publish 는 ack 이 없어 어디에도 걸리지 않는다. payload 도 어긋나 있었다. FE `CardGameState` 에 `FIRST_LOADING` 이 없었고, `BlockStackingGameState` 는 FE 가 `DESCRIPTION`, BE 가 `READY` 였으며, `Player.userId` 는 게스트에서 null 인데 non-null 로 선언돼 있었다.

문제의 뿌리는 도구의 품질이 아니라 방향이다. **조회(pull) 도구는 누군가 기억해서 불러야 동작한다.** 검증은 실패해서 알려주는 push 여야 한다.

## 결정

조회 MCP 를 폐기하고, BE 가 FE 타입 파일을 생성해 tsc 가 계약을 강제하게 한다.

1. **원천 검증 (`WsCatalogContractTest`, `:app`)** — `@WsTopic.path` 리터럴과 실제 발행 경로 상수를 양방향 대조하고, `@MessageMapping` 의 `@WsReceive` 누락을 잡는다.
2. **생성물 신선도 (backend-ci)** — 같은 테스트가 `ws-catalog.json` fixture 와 `frontend/src/apis/websocket/generated/` 의 `wsContract.ts`·`ws-openapi.json` 을 항상 다시 쓴다. 커밋본과 다르면 `git diff --exit-code` 가 실패한다. 조건부 실행 플래그를 없앤 것이 핵심이다. 그게 "CI 에서 절대 안 도는" 원인이었다. `ws-openapi.json` 에서 나오는 `wsOpenApi.d.ts` 는 node 가 있는 frontend-ci 가 `npm run generate:ws` 를 돌려 같은 방식으로 대조한다.
3. **FE 타입 게이트 (tsc)** — 생성 파일이 destination union 과 payload 타입, 그리고 destination 에서 payload 를 찾는 `WsPayloadOf<D>` 를 담는다. `useWebSocketSubscription` 과 `send` 의 파라미터 타입이 그것을 받으므로, 카탈로그에 없는 경로나 어긋난 필드는 컴파일 오류다. frontend-ci 는 `ts-loader` 전체 타입검사로 빌드하므로 검사 코드나 CI 배선이 따로 없다.

생성 파일이 `frontend/` 아래에 있는 것이 의도다. BE 가 계약을 바꾸면 그 PR 에서 frontend-ci 가 돌고, 깨진 FE 호출부가 같은 PR 에서 드러난다. 통합 브랜치라서 가능한 결합이다.

### 왜 AST 스캔 테스트가 아닌가

처음 설계는 FE 호출부를 TypeScript AST 로 긁어 fixture 와 대조하는 jest 테스트였다(닫은 PR #1735). 세 가지 이유로 바꿨다.

- 검사 코드가 0줄이다. 타입만 있으면 컴파일러가 검사한다.
- destination 을 상수 모듈로 옮기면 AST 스캔은 눈이 먼다. 타입은 값이 어디서 오든 따라간다.
- payload 까지 같은 파일로 덮는다. AST 스캔은 destination 문자열만 본다.

### `${string}` 이 `/` 를 삼키는 문제

경로 변수는 `${string}` 이 되는데, 이 타입은 `/` 도 포함하므로 `/room/${string}` 하나가 `/room/x/ladder/typo` 에도 맞는다. union 대입만으로는 오타를 못 잡는다. 호출부 리터럴이 정확히 한 패턴과 같을 때(상호 대입)만 통과시키는 조건부 타입 `Exact<D, P>` 를 생성 파일에 함께 낸다. 실패하면 틀린 경로가 찍힌 문자열 리터럴 타입이 되어 오류 메시지에서 원인이 보인다. `[D] extends [Extract<P, D>]` 한 줄 형태는 훅 구현부에서 `TS2590` 이 나서 3단 조건부 타입을 유지한다.

### payload 타입은 직접 변환하지 않고 OpenAPI 를 거친다

Java record 를 TS 로 옮기는 코드를 직접 두면 `Map`·중첩 제네릭·다형 payload 같은 모양이 나올 때마다 그 코드가 자란다. 대신 springdoc 이 REST 문서에 쓰는 swagger-core `ModelConverters` 로 카탈로그 참조 record 를 OpenAPI `components.schemas` 로 내고(`ws-openapi.json`, paths 는 빈 객체), FE 의 `openapi-typescript` 가 그 문서에서 `wsOpenApi.d.ts` 를 만든다. REST 계약(#1741)과 같은 도구다. `wsContract.ts` 는 destination union·`WsPayloadOf` 와 `export type X = components['schemas']['X']` 이름 alias 만 담아, FE 는 import 경로 하나로 둘 다 받는다.

직접 쓰는 코드는 두 도구를 잇는 접착뿐이다. swagger-core 는 enum 을 필드마다 인라인으로 풀므로 `ModelResolver.enumsAsRef` 로 이름 있는 스키마로 내고(FE 가 enum 이름을 import 한다), `JsonNode` 는 빈으로 풀면 가짜 스키마가 되어 모양 없는 `object` 로 둔다. 이 프로젝트는 Jackson 3 라 `tools.jackson.databind.JsonNode` 를 잡아야 한다.

### null 여부는 `@Nullable` 로 표시한다

래퍼 타입(`Long`·`Integer`)만으로 `| null` 을 붙이면 실제로 null 이 안 오는 필드까지 FE 곳곳에서 거짓 오류가 난다. 리플렉션으로는 null 여부를 알 수 없으므로 BE 가 `@Nullable`(jspecify) 을 단 컴포넌트만 카탈로그 타입 문자열 뒤에 `?` 를 붙인다. swagger-core 는 `required` 를 내지 않아 그대로 두면 모든 필드가 선택이 되므로, `WsOpenApiEmitter` 가 그 표시로 `required` 와 `nullable` 을 채운다. `$ref` 옆의 `nullable` 은 OAS 3.0 에서 무시되어 `allOf` 로 감싼다. 결과는 `@Nullable` 필드만 `field?: T | null` 이다. `@JsonInclude(NON_NULL)` 이면 필드가 빠지고 아니면 null 이 오므로 둘 다 허용한다. **null 을 넘기는 record 컴포넌트에는 `@Nullable` 을 단다.** 이것이 이 ADR 이 BE 에 새로 요구하는 규칙이다.

enum 을 `String` 으로 지운 필드(`SpeedTouchStateResponse.state` 등)는 enum 타입으로 바꿨다. 지운 채로는 생성해도 `string` 이라 enum 검사가 안 된다. JSON 은 같다.

## ADR-0012 의 스냅샷 검증 거부를 뒤집는다

ADR-0012 는 fixture 스냅샷 동등 검증을 두지 않기로 했다. 근거 둘이 모두 소멸했다.

- "MCP 가 라이브 엔드포인트를 직접 소비하므로 fixture 가 계약을 강제할 근거가 없다." MCP 가 사라지고 생성물이 FE 게이트의 SSOT 가 됐다.
- "OS 별 줄바꿈 차이로 인한 불안정성." `DefaultIndenter("  ", "\n")` 와 `.gitattributes eol=lf` 로 제거했다.

같은 ADR 이 "안정 정렬로 출력이 결정적"이라 적었지만 세 곳이 순서 의존이었다. `mergeTopicGroup`·`mergeQueueGroup` 이 정렬 없이 grouping 해 `getFirst().payloadType()` 과 `referencedSchemas` 가 `getDeclaredMethods()` 순서를 탔고, `sends` 정렬에 타이브레이커가 없었다. 함께 고쳤다.

## 고려한 대안

- **AST 스캔 jest 테스트**: 위 "왜 AST 스캔 테스트가 아닌가". **기각**.
- **FE 쪽 스크립트가 fixture 를 TS 로 변환**: BE PR 이 fixture 를 바꾸면 FE 생성물이 낡아 BE 작업자가 FE 스크립트를 한 번 더 돌려야 한다. destination 파일은 BE 테스트가 바로 쓴다. payload 는 `openapi-typescript` 가 FE 에서 돌지만 pre-push 훅이 컨트랙트 테스트 뒤에 이어 돌리고 frontend-ci 가 신선도를 대조하므로 한 번 더 돌릴 일은 없다. **부분 채택**.
- **Java→TS 변환기를 직접 유지**: 첫 구현이 이랬다(`WsContractTsEmitter` 가 record·enum·`List`·`Map` 을 TS 로 옮김). 다루는 모양이 늘 때마다 자라는 코드라 swagger-core + `openapi-typescript` 로 바꿨다. **기각**.
- **typescript-generator (cz.habarta)**: Java 클래스에서 TS 를 바로 내는 성숙한 플러그인이다. 다만 REST 가 `openapi-typescript` 를 쓰므로 같은 역할의 도구가 둘이 되고 이름·nullability·enum 표현이 갈린다. **기각**.
- **래퍼 타입을 전부 `| null` 로**: FE 15곳 이상에서 거짓 오류. **기각**, `@Nullable` 표시로 대체.
- **BE+FE 를 띄우는 E2E**: 런타임 전체를 덮지만 실행당 4~6분, 호스트·게스트 2컨텍스트, 게임 타이머 flaky. 정적 게이트가 잡는 것을 E2E 로 잡을 이유가 없다. dev push 시 도는 얇은 스모크 E2E 는 별도 이슈로 남긴다.
- **Pact 메시지 계약**: 계약 파일을 저장소에 커밋하면 브로커 없이도 돌고, STOMP 전송과 무관하게 payload 형태는 검증한다. 다만 FE 가 기대값을 손으로 적는 소비자 주도 방식이라 지금 손글씨 타입과 같은 약점이 남고, tsc 게이트를 잃는다. **기각**.
- **Springwolf + AsyncAPI Modelina**: `@MessageMapping`·`@SendTo` 를 스캔해 AsyncAPI 문서를 내고 Modelina 가 TS payload 타입을 만든다. 카탈로그를 대체하지만 destination union 과 동치 검사는 어차피 직접 써야 하고, 템플릿 발행부에 `@AsyncPublisher` 규약이 새로 든다. payload 는 swagger-core + `openapi-typescript` 가 이미 덮는다. **보류**.

## 결과

- **얻는 것**: destination 과 payload drift 가 머지 전에 컴파일 오류로 잡힌다. 첫 적용에서 drift 5건(`racing-game/start`, `FIRST_LOADING`, `BlockStackingGameState`, `Player.userId`, `currentRound` 의 `READY`)을 잡았다. api-mcp 소스와 의존성, CI 워크플로가 사라진다.
- **잃는 것**: 실행 중인 서버에 STOMP 로 직접 찔러보는 능력. 필요하면 BE 통합 테스트(`TestStompSessionFactory`)를 쓴다.
- **부작용**: `./gradlew test` 가 fixture 와 FE 생성물을 다시 써서 소스 트리를 더럽힌다. 더러워졌다는 건 생성물이 낡았다는 뜻이다. `wsOpenApi.d.ts` 는 `npm run generate:ws` 를 따로 돌려야 하고, pre-push 훅이 이어 돌린다.
- **알려진 한계**: 방 코드를 고정한 문자열(`/room/ABCD/winner`)은 정상 경로여도 동치 검사에 걸린다. 호출부는 변수를 보간해 넘긴다. 눈치게임은 BE 가 평탄한 record 로 보내고 FE 는 ADR-0031 의 state 별 union 으로 읽으므로 한 번 캐스팅한다. `SeasonRankMessage.Entry.tier` 는 BE 도 `String` 이라 좁히지 못했다.

## 다음 단계

- **`send` body 타입**: `WsRequestOf<D>`. `MiniGameMessage.commandRequest` 가 `JsonNode` 라 모양 없는 `object` 가 되어 지금 호출부가 깨진다. BE 가 command 별 타입을 드러낸 뒤에 한다(#1742).
- **컨트랙트 테스트 경량화**: 애노테이션 스캔과 스키마 생성만 하는데 Spring 컨텍스트와 Testcontainers 를 띄운다. Docker 없이 돌게 하면 pre-push 훅을 조건 없이 매번 돌릴 수 있다.
- **REST 계약 (#1741)**: `/v3/api-docs` 를 같은 테스트에서 받아 `frontend/` 아래에 쓰고, 타입은 `openapi-typescript` 로 만든다. REST 는 기성품이 있으므로 생성기를 직접 쓰지 않는다.
- **스모크 E2E**: dev push 트리거, Playwright 2컨텍스트, 방 생성부터 레이싱 PLAYING 까지.

## 변경 범위

- 신설: `backend/app/src/test/java/coffeeshout/contract/WsCatalogContractTest.java`, `WsContractTsEmitter.java`, `WsOpenApiEmitter.java`, `frontend/src/apis/websocket/generated/`(`wsContract.ts`·`ws-openapi.json`·`wsOpenApi.d.ts`), `frontend` 의 `openapi-typescript` devDependency 와 `npm run generate:ws`
- 수정: `WsCatalogBuilder`(결정론 3곳, `@Nullable` → `?`), 게임 response record 의 enum·`@Nullable`, `useWebSocketSubscription`·`useUserSocketSubscription`·`WebSocketContext`, `frontend/src/types/**`(생성 타입 alias), `.gitattributes`, `backend-ci.yml`, `frontend-ci.yml`, `.githooks/pre-push`, `ws-contract` 스킬, `fe-code-reviewer` 에이전트
- 삭제: `tools/api-mcp/`, `.github/workflows/api-mcp-ci.yml`, `backend/.mcp.json`, `frontend/.mcp.json`, `frontend/.claude/skills/api-contract/`, `WsCatalogFixtureGeneratorTest`, dependabot 의 `/tools/api-mcp` 항목
