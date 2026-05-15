# 0011. WebSocket 컨트랙트 디스커버리 — `@WsTopic` + `/dev/ws-catalog`

- 날짜: 2026-05-14
- 상태: 보류

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

자체 `@WsTopic` 어노테이션과 dev 전용 카탈로그 엔드포인트 `GET /dev/ws-catalog`를 도입한다.
Node 기반 MCP 서버(`tools/ws-mcp/`)가 이 엔드포인트를 소비해 컨트랙트 디스커버리와 연결 검증을 한 번에 제공한다.

**`@WsTopic` 어노테이션 시그니처** (`coffeeshout.global.websocket.docs.WsTopic`)

```java
@Target(METHOD) @Retention(RUNTIME)
public @interface WsTopic {
    String path();                          // 응답 토픽 경로 (e.g. "/room/{joinCode}")
    Class<?> payload();                     // 페이로드 DTO
    Class<?> generic() default Void.class;  // List<X> 등의 X
    String description() default "";        // legacy @Operation 의 summary/description 흡수
}
```

순수 Java 어노테이션으로 외부 라이브러리/Spring 의존성을 0으로 유지한다.
멀티 모듈 분리가 시작되면 이 어노테이션만 `ws-contract` 모듈로 떼어내 모든 도메인 모듈이 의존하도록 한다.

**`@WsReceive` 어노테이션 시그니처** (`coffeeshout.global.websocket.docs.WsReceive`)

```java
@Target(METHOD) @Retention(RUNTIME)
public @interface WsReceive {
    String[] respondsOnTopics() default {};  // 후속 발행 토픽 경로 (선언적 문서화 전용)
    String description() default "";
}
```

`@MessageMapping`만 있는 핸들러 — 즉 직접 토픽을 발행하지 않고 Redis Stream 이벤트를 발행해 비동기로 응답이 나가는 핸들러 — 에 사용한다.
`@WsTopic(payload = Object.class)` 형식의 회피 코드를 막기 위해 도입되었다.
`respondsOnTopics`로 이 send 처리 결과가 어떤 토픽으로 최종 발행되는지 선언적으로 표시해 FE가 send→topic 인과 관계를 파악할 수 있도록 한다.
request payload 식별은 `@Payload` 어노테이션을 통해 명시한다 — `@Payload` 없는 매개변수는 카탈로그에서 `requestType=null` 로 표시된다.

**`WsCatalogBuilder`** (`coffeeshout.global.websocket.docs.WsCatalogBuilder`)

- `ApplicationContext.getBeansWithAnnotation(Component.class)`로 빈을 스캔한다. `@WsTopic`을 붙이는 주체가 `@Controller` 뿐 아니라 `RoomMessagePublisher` 같은 `@Component` Publisher도 있기 때문이다 (패키지 prefix 의존 금지).
- `@MessageMapping` 메서드의 send destination 과 `@WsTopic` 메서드의 response topic 메타데이터를 함께 수집한다.
- Jackson `JavaType` introspection 으로 페이로드 스키마(필드명/타입)를 1단계까지 추출하고, 그 이상의 중첩은 `"$ref": "ClassName"` 으로 표기한다.

**`WsCatalogController`** (`coffeeshout.global.websocket.docs.WsCatalogController`)

- `@RestController` + `@Profile("!prod")` 이중 가드로 운영 환경 노출을 방지한다.
- `GET /dev/ws-catalog` 가 카탈로그 JSON 을 반환한다.
- `application.yml` 의 `websocket.docs.info.*` 값을 `@ConfigurationProperties("websocket.docs")` 로 메타에 포함한다.

**legacy 의존성 제거**

- `build.gradle.kts` 의 `io.github.20hyeonsulee:websocket-docs-generator` 의존성을 제거한다.
- 9개 컨트롤러/Publisher 의 `@MessageResponse`/`@Operation` 을 `@WsTopic` 으로 치환한다.
- `@JsonSchemaEnumType` 은 단일 사용처 (`RacingGameStateResponse`) 에서 제거하고 description 으로 의미를 옮긴다.

## 고려한 대안

| 대안 | 장점 | 단점 |
|-----|----|----|
| legacy 라이브러리 유지 | 변경 없음 | 비관리 외부 라이브러리, 정적 HTML, FE 워크플로우 미통합 |
| Springdoc / AsyncAPI 도입 | 표준 스펙 | 도입 비용 크고 STOMP 매핑/Redis Stream 트리거 등 자체 컨벤션을 표현하기 어려움 |
| 자체 `@WsTopic` + JSON 엔드포인트 + MCP | 메타데이터 자체 소유, MCP 로 FE/Claude 둘 다 소비, 멀티 모듈 분리 내성 | 어노테이션 마이그레이션 비용 (9개 파일) |

## 트레이드오프

**장점**

- 카탈로그 메타데이터를 외부 라이브러리에 의존하지 않고 직접 소유한다.
- JSON 엔드포인트라 FE codegen, MCP 디스커버리, CI 산출물 등 후속 활용이 모두 가능하다.
- 빌더가 패키지 prefix 가 아닌 ApplicationContext 빈 스캔이므로 향후 도메인 모듈 분리에도 영향을 받지 않는다.

**단점**

- 9개 컨트롤러/Publisher 의 어노테이션 일괄 치환 비용이 든다 (1회성).
- 정적 HTML 산출물(빌드 사이트 게시)이 사라진다. 후속 PR 에서 CI artifact 로 `ws-catalog.json` 을 게시하는 방식으로 대체한다.
- `@RestController @Profile("!prod")` 의 dev 노출 정책을 운영자가 알아야 한다 (DEV 환경에서만 동작).

## 결과

- `coffeeshout.global.websocket.docs` 패키지에 4개 신규 클래스 추가.
- 9개 도메인 컨트롤러/Publisher 가 새 어노테이션을 사용한다.
- `build.gradle.kts` 에서 legacy 의존성 라인 1개 제거.
- `application.yml` 의 `websocket.docs.*` 키는 유지하되 `enabled` 키만 우리 컨트롤러의 활성화 플래그로 의미가 변경된다 (Profile 가드와 함께 이중 토글).
- 후속 PR 에서 Node MCP 서버 (`tools/ws-mcp/`) 와 `frontend/CLAUDE.md` 가이드를 추가한다.
- 멀티 모듈 마이그레이션이 시작되면 `@WsTopic` 만 별도 `ws-contract` 모듈로 떼어내 모든 도메인 모듈에서 의존한다.
