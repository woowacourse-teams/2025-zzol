# ADR-0017: `IntegrationTestSupport` WebEnvironment 전략 — `MOCK` 기본, `RANDOM_PORT` 명시 오버라이드

## 상태

적용됨 (2026-05-28)

## 컨텍스트

ADR-0015에서 정의한 `IntegrationTestSupport` 계층은 `RANDOM_PORT`를 기본값으로 사용했다.
그러나 대부분의 통합 테스트는 `MockMvc`만 사용하며, 실제 TCP 소켓을 통한 HTTP 라운드트립이 필요하지 않다.

`RANDOM_PORT + MockMvc` 조합은 둘 다의 단점만 갖는다.

- `MockMvc`는 `DispatcherServlet`을 직접 호출하므로 `RANDOM_PORT`여도 실제 TCP를 거치지 않는다.
- Tomcat 부트스트랩 비용(포트 바인딩, 서블릿 컨테이너 초기화)은 그대로 발생한다.

반면 아래 두 경우는 반드시 `RANDOM_PORT`가 필요하다.

- WebSocket/STOMP 테스트: `ws://localhost:{port}/ws` 실제 연결 (`@LocalServerPort`)
- `TestRestTemplate` / `WebTestClient`: 실제 HTTP 소켓을 통한 응답 검증

## 결정

`WebEnvironment` 기본값을 **`MOCK`** 으로 내린다. `RANDOM_PORT`가 필요한 `:app` 모듈은 명시적으로 오버라이드한다.

```text
test-support/IntegrationTestSupport   @SpringBootTest(MOCK)   ← 기본값
  │
  ├── user/UserModuleIntegrationTest  @SpringBootTest(MOCK)   ← 명시 (classes 지정 필요)
  │
  └── app/IntegrationTestSupport      @SpringBootTest(RANDOM_PORT) ← 명시 오버라이드
        (WebSocket + TestRestTemplate 테스트 존재)
```

### `MOCK`을 사용하는 경우 (기본)

- 컨트롤러 + 서비스 + DB 레이어 통합 검증
- Security 필터, `@ControllerAdvice`, 인터셉터 포함 전체 Spring MVC 파이프라인 검증
- `MockMvc`로 충분한 모든 경우

### `RANDOM_PORT`를 사용하는 경우 (명시 필요)

- STOMP over WebSocket 연결 (`StandardWebSocketClient`, `WebSocketStompClient`)
- `TestRestTemplate` · `WebTestClient`로 실제 HTTP 소켓 응답 검증
- 리디렉션 follow, chunked 인코딩 등 raw HTTP 동작 검증

## 고려한 대안

### 대안 A: 전 모듈 `RANDOM_PORT` 유지

- 장점: 변경 없음
- 단점: Tomcat 부팅 비용 낭비, `RANDOM_PORT + MockMvc` 안티패턴 지속
- 기각: 불필요한 빌드 시간 증가

### 대안 B: `:app`도 `MOCK`으로 내리고, WebSocket/HTTP 테스트만 별도 베이스 클래스 분리

- 장점: `:app` MockMvc 테스트도 빠른 환경으로
- 단점: WebSocket 테스트 계층 재편 필요, 변경 범위 큼
- 보류: 필요 시 추후 적용

## 결과

- 도메인 모듈(`:user`, `:room` 등)의 `IntegrationTestSupport` 기반 테스트에서 Tomcat 부팅 비용 제거
- `:app`의 WebSocket + `TestRestTemplate` 테스트는 `RANDOM_PORT`를 유지해 동작 영향 없음
- 새 도메인 모듈 통합 테스트는 `MOCK`을 기본으로 사용하며, 실제 HTTP가 필요할 때만 명시적 오버라이드
