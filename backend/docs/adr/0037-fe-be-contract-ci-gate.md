# 0037. 조회형 api-mcp 를 폐기하고 FE↔BE 계약을 CI 게이트로 강제

- 날짜: 2026-09-01
- 상태: 적용됨 (이슈 #1733)

## 컨텍스트

ADR-0012 가 `@WsTopic`·`@WsQueue`·`@WsReceive` 애노테이션과 `GET /dev/ws-catalog` 로 WebSocket 계약을 기계가 읽을 수 있게 만들었고, ADR-0029 가 그 소비자인 MCP 서버에 HTTP/OpenAPI 도구를 더했다. 두 ADR 모두 동기를 같은 문장으로 적었다. `fe/dev` 와 `be/dev` 가 분리돼 있어 FE 가 BE 코드를 직접 읽을 수 없다는 것이다.

그 전제가 사라졌고, 남은 도구는 쓰이지 않았다.

- **전제 소멸**: 커밋 `64377832` 로 `dev` 통합 브랜치가 생겨 FE 작업 중에도 BE 소스를 그대로 읽는다. 조회 도구가 대체하던 일을 `grep` 이 한다.
- **실사용 0건**: 전체 세션 트랜스크립트에 `mcp__api__*` 도구 호출 기록이 없다. 캐시 디렉터리 `~/.zzol-mcp/` 는 생성된 적조차 없어, 조회 도구가 한 번도 성공적으로 돈 적이 없음을 뜻한다. 서버는 연결됐는데 아무도 부르지 않았다.
- **유지 비용은 실재**: 소스 1,731 줄, 의존성 14 개, 전용 CI 워크플로 1 개. 기능 개발은 2026-06-20 에 멈췄고 이후 두 달간 dependabot PR 만 12 건 쌓였다.

동시에, 계약이 어긋난 코드가 아무 저항 없이 배포돼 있었다. `RacingGamePlayPage.tsx` 가 `/app/room/{joinCode}/racing-game/start` 로 보내는데 BE 에는 `racing-game/tap` 핸들러밖에 없다. STOMP 는 없는 토픽도 구독에 성공하고 `publish` 는 ack 이 없어, 이런 drift 는 빌드·린트·테스트·런타임 어디에도 안 걸리고 사용자만 화면이 안 넘어가는 걸 본다.

계약 아티팩트도 썩어 있었다. 커밋된 `ws-catalog.json` 은 topics 17 / queues 5 / sends 12 인데 소스는 23 / 6 / 14 였다. 생성기 `WsCatalogFixtureGeneratorTest` 가 `-DupdateFixture=true` 없이 스킵되는데 CI 에 그 플래그가 없었고, `:websocket` 모듈에 있어 상대경로가 커밋본이 아닌 파일을 가리켰으며, 그 모듈 테스트 컨텍스트에는 게임 Notifier 빈이 없어 실행해도 카탈로그가 비었다. fixture 는 그동안 사람이 손으로 편집돼 왔다.

문제의 뿌리는 도구의 품질이 아니라 방향이다. **조회(pull) 도구는 누군가 기억해서 불러야 동작한다.** 호출 0 건이 그 방식의 결론이다.

## 결정

조회 MCP 를 폐기하고, 같은 계약 원천을 CI 가 강제하는 3 단 게이트로 재배치한다. 검증은 사람이 부르는 게 아니라 실패해서 알려주는 것이어야 한다.

1. **원천 검증 (`WsCatalogContractTest`, `:app`)** — `@WsTopic.path` 리터럴과 실제 발행 경로 상수를 양방향 대조한다. 둘은 지금까지 사람이 맞춰 온 별개의 문자열이다. `@MessageMapping` 의 `@WsReceive` 누락도 함께 잡는다. 기존 `log.warn` 은 그대로 둔다. 기동 예외로 올리면 모든 모듈의 Spring 컨텍스트가 문서 메타데이터 문제로 죽는다.
2. **아티팩트 신선도 (backend-ci)** — 테스트가 카탈로그를 fixture 로 항상 다시 쓰고, 커밋본과 다르면 `git diff --exit-code` 가 실패한다. 조건부 실행 플래그를 없앤 게 핵심이다. 그게 "CI 에서 절대 안 도는" 원인이었다.
3. **FE 대조 (`wsContract.test.ts`, frontend-ci)** — TypeScript AST 로 구독·발행 호출부의 destination 을 뽑아 fixture 와 대조한다. prefix 규칙은 런타임 코드(`isBrokerDestination`, `WEBSOCKET_CONFIG`)를 import 해 재사용한다. 복붙하면 규칙이 바뀌는 순간 검사가 거짓말을 시작한다.

`tools/api-mcp` 는 전량 삭제한다. `.mcp.json` 2 개, `api-mcp-ci.yml`, dependabot 항목, `api-contract` 스킬도 함께 지운다. `ws-contract` 스킬은 조회 대상을 fixture 로 바꿔 유지한다. "destination 을 쓰기 전에 계약부터 확인한다"는 워크플로 자체는 여전히 옳다.

## ADR-0012 의 스냅샷 검증 거부를 뒤집는다

ADR-0012 는 fixture 스냅샷 동등 검증을 두지 않기로 명시했다. 근거가 둘이었고 지금은 둘 다 성립하지 않는다.

**"MCP 가 라이브 엔드포인트를 직접 소비하므로 fixture 가 계약을 강제할 근거가 없다."** 그 MCP 가 사라지고 fixture 가 FE 게이트의 SSOT 가 된다. 이제 fixture 의 신선도가 곧 FE 검사의 정확도다. 낡은 fixture 위에서 검사를 돌리면 정상 destination 을 오탐한다.

**"OS 별 줄바꿈 차이로 인한 불안정성 비용이 더 크다."** `DefaultIndenter("  ", "\n")` 로 줄바꿈을 고정하고 `.gitattributes` 에 `eol=lf` 를 더해 제거했다.

같은 ADR 이 "publishers 와 schemas 를 안정 정렬해 출력이 결정적임을 보장한다"고 적었지만 실제로는 세 곳이 순서 의존이었다. `mergeTopicGroup`·`mergeQueueGroup` 이 정렬 없이 grouping 해 대표 `payloadType`(`getFirst()`)과 `referencedSchemas` 가 `getDeclaredMethods()` 순서를 탔고, `sends` 정렬에 타이브레이커가 없었다. 병합 전 발행 지점 정렬과 타이브레이커를 더해 함께 고쳤다. macOS 와 우분투 러너에서 재컴파일 포함 실행이 바이트 동일함을 확인했다.

## 고려한 대안

- **애노테이션과 발행 상수를 하나로 합치는 리팩터링**: `@WsQueue` 가 이미 쓰는 방식이고 구조적으로 더 낫다. 다만 프로덕션 13 개 클래스 400 줄 이상을 건드리는데, `QrCodeSubscriptionHandler` 처럼 애노테이션 없이 상수만 두고 발행하는 경우는 여전히 못 잡아 검사를 남겨야 한다. 큰 diff 를 내고도 검사가 필요하면 검사만 두는 게 맞다. **보류**. 이 게이트가 초록인 상태에서 나중에 안전하게 할 수 있다.
- **FE 검사를 ESLint 커스텀 룰로**: 에디터 즉시 피드백을 얻는다. 대가는 로컬 플러그인 인프라 신설이다(현재 커스텀 플러그인 0 개, 타입인지 린팅 비활성). destination 오타는 몇 주에 한 번 나는 실수라 그 비용을 못 넘는다고 봤다. **기각**. 나중에 얹고 싶으면 이 테스트의 판정 함수를 룰에서 재사용하면 된다.
- **FE 검사가 라이브 `/dev/ws-catalog` 를 소비**: 항상 최신이지만 frontend-ci 에서 BE 를 기동해야 하고, 서버가 안 뜨면 검사가 통째로 스킵되거나 실패한다. **기각**.
- **`ws_connect`·`ws_subscribe`·`ws_send` 3 종 존치**: STOMP 핸드셰이크는 `curl` 로 못 해서 유일하게 대체 불가한 능력이다. 다만 BE 에 `TestStompSessionFactory` 가 같은 일을 하고 두 달간 호출이 0 건이었다. **기각**. 필요해지면 `git show` 로 되살린다.

## 결과

- **얻는 것**: 계약 drift 가 머지 전에 빨간 CI 로 잡힌다. 첫 적용에서 실제 drift 1 건(`racing-game/start`)과 fixture 6 개 토픽 누락을 잡았다. 소스 1,731 줄과 의존성 14 개, CI 워크플로 1 개가 사라진다.
- **잃는 것**: 실행 중인 서버에 STOMP 로 직접 찔러보는 능력. 필요하면 BE 통합 테스트를 쓴다.
- **부작용**: `./gradlew test` 가 fixture 를 다시 써서 소스 트리를 더럽힌다. `spotlessApply` 와 같은 성격이고, 더러워졌다는 건 fixture 가 낡았다는 뜻이다.
- **알려진 한계**: FE 검사는 destination 을 정적으로 판정할 수 있을 때만 동작한다. 지금은 31 개 호출부가 전부 정적 리터럴이지만 이건 설계된 상태가 아니라 우연이다. destination 상수 모듈을 만드는 리팩터링을 하면 호출부가 전부 식별자가 되어 검사가 눈을 감는다. 판정 불가를 실패로 두고 `// ws-contract-ignore` 주석으로만 넘기게 한 이유다. **예외 주석이 2 개 이상 생기면** 게이트를 완화할 게 아니라 상수 정의를 순회하는 형태로 전환한다.

## 다음 단계

- **payload 타입 대조**: fixture 에 `payloadType` 과 `schemas` 가 이미 있고 같은 AST 순회에서 `useWebSocketSubscription<T>` 의 타입 인자를 뽑을 수 있다. 다만 lint 보다 카탈로그에서 `.d.ts` 를 생성하는 쪽이 낫다. 생성하면 검증할 대상 자체가 없어진다.
- **REST 경로 대조**: 이 구조가 그대로 받지 못한다. fixture 에 HTTP 가 없어 springdoc `/v3/api-docs` 스냅샷이라는 별도 아티팩트와 별도 신선도 게이트가 먼저 필요하다.

## 변경 범위

- 신설: `backend/app/src/test/java/coffeeshout/contract/WsCatalogContractTest.java`, `frontend/src/apis/websocket/__tests__/wsContract.test.ts`
- 수정: `WsCatalogBuilder`(결정론 3 곳), `.gitattributes`, `backend-ci.yml`, `frontend-ci.yml`, `ws-contract` 스킬, `fe-code-reviewer` 에이전트
- 삭제: `tools/api-mcp/`, `.github/workflows/api-mcp-ci.yml`, `backend/.mcp.json`, `frontend/.mcp.json`, `frontend/.claude/skills/api-contract/`, `WsCatalogFixtureGeneratorTest`, dependabot 의 `/tools/api-mcp` 항목
