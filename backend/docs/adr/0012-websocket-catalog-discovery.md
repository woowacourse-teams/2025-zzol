# 0012. WebSocket 컨트랙트 디스커버리 — `@WsTopic`/`@WsQueue`/`@WsReceive` + `/dev/ws-catalog`

- 날짜: 2026-05-14
- 상태: 적용됨 (WsCatalog 구현 반영; 2026-05-16 개정 — `@WsQueue`/`@WsReceive` 추가, 다중 발행자 표시 방식 정리, `envelope-class` 도입 및 `info` 섹션 제거; 2026-05-17 개정 — `WsCatalogSecurityConfig` IP 허용 목록 가드 추가, `generic=Object.class` 거부 보완, fixture 스냅샷 검증 테스트 제거)

## 컨텍스트

STOMP 기반 WebSocket이 zzol의 핵심 통신 수단이고, 게임이 늘어남에 따라 토픽/페이로드도 빠르게 증가하고 있다.
현재 컨트랙트 디스커버리에 세 가지 통증이 있다.

**1. legacy `websocket-docs-generator` 유지 비용**

`io.github.20hyeonsulee:websocket-docs-generator:1.0.7` 가 정적 HTML 문서를 만들지만 더 이상 관리되지 않는 외부 라이브러리다.
9개 컨트롤러/Publisher에 `@MessageResponse(path, returnType, genericType)`, `@Operation(summary, description)`,
`@JsonSchemaEnumType(enumType)` 어노테이션이 흩어져 있지만 산출물이 빌드 사이트에 게시되는 흐름이 없어 활용처가 죽어있다.

**2. FE/BE 분리 워크플로우에서의 컨트랙트 단절**

PR 템플릿 정책상 `fe/dev`, `be/dev`가 분리되어 있고 `frontend/` 디렉토리(`hooks`, `apis`, `types`)는 `.gitkeep`만 있는 그린필드다.
FE 개발자는 BE에 어떤 토픽이 있는지, 페이로드 스키마가 무엇인지, 어떤 트리거로 응답이 발행되는지 파악할 방법이 없다.
정적 HTML 사이트는 GitHub 워크플로우에 통합되지 않아 fe/be 분리 흐름에 맞지 않는다.

**3. 멀티 인스턴스 운영에서의 디버깅 어려움**

ADR-0010 으로 `StompSessionManager`가 Redis 기반으로 전환된 뒤 세션 상태는 인스턴스 간 공유되지만, "지금 어떤 토픽으로 어떤 페이로드를 보내야 하는가"라는 정적 컨트랙트는 여전히 코드를 직접 읽어야 알 수 있다.

## 결정

자체 `@WsTopic`/`@WsQueue`/`@WsReceive` 어노테이션과 dev 전용 카탈로그 엔드포인트 `GET /dev/ws-catalog`를 도입한다.
Node 기반 MCP 서버(`tools/ws-mcp/`)가 이 엔드포인트를 소비해 컨트랙트 디스커버리와 연결 검증을 한 번에 제공한다.

**`@WsTopic` 어노테이션 시그니처** (`coffeeshout.websocket.docs.WsTopic`)

```java
@Target(METHOD) @Retention(RUNTIME) @Repeatable(WsTopics.class)
public @interface WsTopic {
    String path();                          // 응답 토픽 경로 (e.g. "/room/{joinCode}")
    Class<?> payload();                     // 페이로드 DTO
    Class<?> generic() default Void.class;  // List<X> 등의 X
    String description() default "";        // legacy @Operation 의 summary/description 흡수
}
```

순수 Java 어노테이션으로 외부 라이브러리/Spring 의존성을 0으로 유지한다.

**`@WsReceive` 어노테이션 시그니처** (`coffeeshout.websocket.docs.WsReceive`)

```java
@Target(METHOD) @Retention(RUNTIME)
public @interface WsReceive {
    String[] respondsOnTopics() default {};  // 후속 발행 토픽 경로 (선언적 문서화 전용)
    String description() default "";
}
```

`@MessageMapping`만 있는 핸들러 — 즉 직접 토픽을 발행하지 않고 Redis Stream 이벤트를 발행해 비동기로 응답이 나가는 핸들러 — 에 사용한다.
`@WsTopic(payload = Object.class)` 형식의 회피 코드를 막기 위해 도입되었다 — 빌더가 `Object.class` payload 를 거부하므로 비동기 핸들러는 `@WsReceive` 를 써야 한다.
`respondsOnTopics`로 이 send 처리 결과가 어떤 토픽으로 최종 발행되는지 선언적으로 표시해 FE가 send→topic 인과 관계를 파악할 수 있도록 한다.
request payload 식별은 `@Payload` 어노테이션을 통해 명시한다 — `@Payload` 없는 매개변수는 카탈로그에서 `requestType=null` 로 표시된다.

**`@WsQueue` 어노테이션 시그니처** (`coffeeshout.websocket.docs.WsQueue`)

```java
@Target(METHOD) @Retention(RUNTIME) @Repeatable(WsQueues.class)
public @interface WsQueue {
    String path();                          // convertAndSendToUser 에 전달하는 큐 경로 (e.g. "/queue/friends/requests")
    Class<?> payload();                     // 페이로드 DTO
    Class<?> generic() default Void.class;  // List<X> 등의 X
    String description() default "";
}
```

`convertAndSendToUser` 기반의 개인 큐 발행 메서드에 사용한다.
`path` 는 `convertAndSendToUser` 에 전달하는 값 그대로 적는다 — 빌더가 `websocket.docs.user-destination-prefix`(`/user`)를 자동으로 prepend 해 FE 구독 경로(`/user/queue/...`)를 생성한다.
`@WsTopic.path` 가 `/topic/` 을 제외한 상대 경로인 것과 달리, `@WsQueue.path` 는 `/queue/` 를 포함한 경로를 그대로 기재한다 (코드 상수와 1:1 대응 우선).

**`WsCatalogBuilder`** (`coffeeshout.websocket.docs.WsCatalogBuilder`)

- `ApplicationContext.getBeansWithAnnotation(Component.class)`로 빈을 스캔한다. `@WsTopic`을 붙이는 주체가 `@Controller` 뿐 아니라 `RoomMessagePublisher` 같은 `@Component` Publisher도 있기 때문이다 (패키지 prefix 의존 금지).
- `@MessageMapping` 메서드의 send destination 과 `@WsTopic` 메서드의 response topic 메타데이터를 함께 수집한다.
- `@WsQueue` 메서드를 스캔해 `userDestinationPrefix + path` 로 FE 구독 경로를 구성한다.
- 페이로드 스키마는 JDK reflection 으로 추출한다. `cls.isRecord()` 인 경우 `getRecordComponents()` 로 필드명/제네릭 타입을 펼치고, `cls.isEnum()` 인 경우 `getEnumConstants()` 로 enum 값을 나열한다. record/enum 이 아닌 도메인 클래스는 `{kind: "object"}` 로만 표시한다.
- 각 `TopicEntry`/`QueueEntry`/`SendEntry` 에 빌더 시점에 계산된 `referencedSchemas: List<String>` 을 동봉한다. MCP/FE 소비자가 payloadType 문자열을 정규식으로 파싱할 필요 없이 해당 엔트리가 참조하는 schema key 들을 그대로 얻어 `WsCatalog.schemas[name]` 으로 조회한다.
- payload validation: `path` 가 비어 있거나 `/` 로 시작하지 않거나 `payload` 또는 `generic` 이 `Void.class` / `Object.class` 인 경우 `SystemException(WsCatalogErrorCode, ...)` 으로 빌드를 실패시킨다 (`Object.class` 회피 차단 — `generic=Object.class` 도 동일 적용). `envelope-class` 가 record 타입이 아닌 경우도 동일하게 실패한다.
- **다중 발행자 표시**: 동일 `path` 를 발행하는 메서드가 여러 개인 경우 (예: `FRIEND_RESPONSES_QUEUE` 가 수락/거절 양쪽에서 발행) 하나의 `TopicEntry`/`QueueEntry` 로 묶고 각 메서드의 `description` + `source` 를 `publishers` 배열에 보존한다. 동일 path 에 서로 다른 payload type 이 선언되면 warn 로그를 남긴다 (첫 선언만 노출).

**`WsCatalogController`** (`coffeeshout.websocket.docs.WsCatalogController`)

- `@RestController` + `@Profile("!prod")` 단일 가드로 운영 환경 노출을 방지한다.
- `GET /dev/ws-catalog` 가 카탈로그 JSON 을 반환한다. 응답에 `body.hashCode()` 기반 ETag 헤더를 부착해 MCP 캐시의 `If-None-Match` → 304 Not Modified 조회를 지원한다. 이 ETag 는 약한 지문(weak validator) 으로, `WsCatalog` record 의 `hashCode()` 는 JVM 구현에 따라 실행마다 달라질 수 있고 서로 다른 카탈로그가 동일한 해시를 생성하는 충돌 가능성이 있다. 강한 유일성이 필요하다면 SHA-256 같은 암호학적 해시로 교체해야 한다. 현재 구현은 "변경 감지용 캐시 최적화" 용도로만 사용하며, 강한 식별자로 오용해서는 안 된다.
- `application.yml` 의 `websocket.docs.*` path/envelope 값을 `@ConfigurationProperties("websocket.docs")` 로 바인딩한다. envelope record 타입은 `envelope-class` 키에 FQCN 으로 명시하고 `Class<?>` 로 바인딩되어, 빌더가 reflection 으로 필드/스키마를 자동 추출한다.

**`WsCatalogSecurityConfig`** (`coffeeshout.global.websocket.docs.WsCatalogSecurityConfig`)

- `@Configuration` + `@Profile("!prod")` + `@Order(0)` 으로 `/dev/**` 경로에 IP 허용 목록 기반 접근 제어를 추가한다.
- `websocket.docs.allowed-ips` 에 허용할 IP/CIDR 목록을 명시한다 (로컬 개발 환경 기본값: `127.0.0.1`, `::1`, `0:0:0:0:0:0:0:1`).
- `@Profile("!prod")` 가 비-prod 환경 노출을 차단하는 1차 가드이고, `WsCatalogSecurityConfig` 의 IP 필터는 스테이징 환경에서의 불필요한 노출을 막는 2차 가드다.
- `IpAddressMatcher` 인스턴스는 `@PostConstruct` 에서 한 번 생성해 재사용한다 (요청마다 재생성 방지).

**legacy 의존성 제거**

- `build.gradle.kts` 의 `io.github.20hyeonsulee:websocket-docs-generator` 의존성을 제거한다.
- 9개 컨트롤러/Publisher 의 `@MessageResponse`/`@Operation` 을 `@WsTopic`/`@WsReceive` 로 치환한다.
- `@JsonSchemaEnumType` 은 단일 사용처 (`RacingGameStateResponse`) 에서 제거한다. `String state` → `RacingGameState state` 로 enum 자체를 페이로드에 직접 노출해 빌더가 schema 의 `values` 에 enum constants 를 자동 등록하도록 한다.

## 고려한 대안

| 대안                                          | 장점                                  | 단점                                                                           |
|---------------------------------------------|-------------------------------------|------------------------------------------------------------------------------|
| legacy 라이브러리 유지                             | 변경 없음                               | 비관리 외부 라이브러리, 정적 HTML, FE 워크플로우 미통합                                          |
| Springdoc / AsyncAPI 도입                     | 표준 스펙                               | 도입 비용 크고 STOMP 매핑/Redis Stream 트리거 등 자체 컨벤션을 표현하기 어려움                        |
| 자체 `@WsTopic`/`@WsQueue` + JSON 엔드포인트 + MCP | 메타데이터 자체 소유, MCP 로 FE/Claude 둘 다 소비 | 어노테이션 마이그레이션 비용                                                              |
| 동일 path 다중 publisher: silent dedupe         | 단순                                  | `FRIEND_RESPONSES_QUEUE` 처럼 한 큐에 여러 발행 시나리오가 정상인 경우 description/source 정보 손실 |
| 동일 path 다중 publisher: `publishers` 배열로 묶음   | 정보 보존                               | TopicEntry/QueueEntry 모델이 조금 복잡                                              |

## 트레이드오프

**장점**

- 카탈로그 메타데이터를 외부 라이브러리에 의존하지 않고 직접 소유한다.
- JSON 엔드포인트라 FE codegen, MCP 디스커버리, CI 산출물 등 후속 활용이 모두 가능하다.
- 빌더가 패키지 prefix 가 아닌 ApplicationContext 빈 스캔이므로 향후 도메인 모듈 분리에도 영향을 받지 않는다.
- `Object.class` 회피를 명시적으로 차단하므로 컨트랙트 정확도가 보장된다.
- 다중 publisher 가 한 path 를 공유하는 정상 케이스의 정보가 보존된다.

**단점**

- 어노테이션 마이그레이션 비용이 든다 (1회성, 약 15개 파일).
- 정적 HTML 산출물(빌드 사이트 게시)이 사라진다. 후속 PR 에서 CI artifact 로 `ws-catalog.json` 을 게시하는 방식으로 대체한다.
- `@RestController @Profile("!prod")` 의 dev 노출 정책을 운영자가 알아야 한다 (DEV 환경에서만 동작).

## 결과

- `coffeeshout.websocket.docs` 패키지에 11개 신규 클래스 추가 (`@WsTopic`, `@WsTopics`, `@WsReceive`, `@WsQueue`, `@WsQueues`, `WsCatalogBuilder`, `WsCatalogController`, `WsCatalogProperties`, `WsCatalog`).
- 어노테이션 부착 컴포넌트 (총 15개):
    - `@WsTopic`: `RoomWebSocketController`, `RoomMessagePublisher`, `CardGameNotifier`, `RacingGameWebSocketController`, `RacingGameMessagePublisher`, `LadderWebSocketController`, `LadderNotifier`, `BlindTimerGameMessagePublisher`, `BlockStackingNotifier`, `SpeedTouchGameMessagePublisher`
    - `@WsReceive`: `BlindTimerGameWebSocketController`, `BlockStackingWebSocketController`, `SpeedTouchGameWebSocketController`, `MiniGameWebSocketController`
    - `@WsQueue`: `FriendNotifier`, `PresenceNotifier`
- `build.gradle.kts` 에서 legacy 의존성 라인 1개 제거.
- `application.yml` 의 `websocket.docs.*` 키를 재구성한다. `app-path`, `topic-path`, `queue-path`, `user-destination-prefix`, `stomp-endpoint`, `error-topic`, `envelope-class`, `allowed-ips` 를 사용하며 `enabled` / `base-package` / `server-url` / `info` 키는 제거한다 (`@Profile("!prod")` 가 dev 가드 단일 책임을 지므로 별도 `info` 메타가 필요 없다).
- 멀티 모듈 분리(`@WsTopic`/`@WsQueue` 등 어노테이션의 별도 모듈 추출)는 본 ADR 범위 밖으로, 멀티 모듈 마이그레이션이 실제로 시작되는 시점에 별도 ADR 로 다룬다.
- Node MCP 서버 (`tools/ws-mcp/`)는 커밋 726a51f0 에서 6종 도구(ws_connect, ws_describe, ws_list_topics, ws_send, ws_source, ws_subscribe)로 추가되었다. roomToken 발급 흐름은 ADR-0009(POST /api/rooms/{joinCode}/session-token) 를 따른다.
- MCP 등록 파일(`.mcp.json`)은 **각 서브프로젝트 폴더**(`backend/.mcp.json`, `frontend/.mcp.json`)에 둔다. Claude Code 는 실행 디렉토리의 `.mcp.json` 만 인식하고 개발자가 보통 `cd backend && claude` / `cd frontend && claude` 흐름으로 띄우므로 모노레포 루트 `.mcp.json` 은 두지 않는다. `args` 의 상대 경로는 `../tools/ws-mcp/dist/server.js` 를 공통 사용한다.
- `frontend/CLAUDE.md` 와 `frontend/.mcp.json` 가이드는 별도 PR(fe/dev 베이스)에서 추가한다.
- `ws-catalog.json` fixture 는 `WsCatalogFixtureGeneratorTest`(`-DupdateFixture=true` 실행)로 재생성한다. 스냅샷 동등 검증 테스트는 두지 않는다 — MCP 서버가 라이브 엔드포인트를 직접 소비하므로 fixture 스냅샷이 계약을 강제할 근거가 없고, OS별 줄바꿈 차이로 인한 불안정성 비용이 더 크다.
- `WsCatalogBuilder` 는 publishers(`className#methodName` 사전순)와 schemas(이름 사전순)를 안정 정렬해 카탈로그 JSON 출력이 결정적임을 보장한다. JVM HashSet/HashMap 의 비결정적 순서가 출력에 새지 않도록 막아 호출마다 동일한 바이트열을 유지한다. 이는 `WsCatalogController` 의 ETag 캐시(`hashCode` 기반 약한 지문)가 의미를 갖기 위한 전제 조건이다.
