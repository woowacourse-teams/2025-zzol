# 기술 스택 & 처리 흐름

## 기술 스택

| 분류         | 기술                                            |
|------------|-----------------------------------------------|
| 프레임워크      | Spring Boot 4.1.1, Java 21                    |
| 실시간 통신     | WebSocket (STOMP) + SockJS                    |
| 메시징        | Redis Stream                                  |
| 캐시 / 분산 락  | Valkey(Redis) + Redisson                      |
| 영속성        | Spring Data JPA + QueryDSL, MySQL 8.0         |
| DB 마이그레이션  | Flyway                                        |
| 서킷 브레이커    | Resilience4j                                  |
| 관찰 가능성     | Micrometer + Prometheus, OpenTelemetry(Tempo) |
| 외부 스토리지    | Oracle Object Storage (QR 코드)                 |
| 테스트        | JUnit Jupiter 6, AssertJ, TestContainers      |

---

## Redis Stream 처리 흐름

```text
[클라이언트 WebSocket]
      │ 메시지 전송 (/app/...)
      ▼
[ui/ Handler]
  커맨드를 받아 BaseEvent 구현체(record) 생성
      │
      ▼
[StreamPublisher.publishAsync(StreamKey, BaseEvent)]
  JSON 직렬화 + 현재 트레이스 컨텍스트를 traceparent 필드로 주입 후 XADD
  XADD 응답을 기다리지 않고 CompletableFuture를 돌려준다 (Handler는 이 future를 반환)
  레코드 구조: MapRecord {payload: <이벤트 JSON>, traceparent: <W3C 헤더>}
      │
      ▼
[Redis Stream (Valkey)]
  StreamKey 별로 독립된 스트림으로 적재
      │
      ▼
[RedisStreamListenerStarter]
  StreamKey별 StreamMessageListenerContainer + 전용 ThreadPool로 폴링
  payload JSON 역직렬화 → @JsonTypeInfo로 구체 타입 복원
  traceparent 필드에서 컨텍스트 복원 → consumer span 스코프 안에서 디스패치
      │
      ▼
[EventDispatcher.handle(streamKey, BaseEvent)]
  ResolvableType으로 Consumer<이벤트타입> 빈을 모두 조회해 순서대로 팬아웃 실행
  한 Consumer의 실패가 나머지 Consumer의 수신을 막지 않는다
      │
      ▼
[Consumer<T> 구현체 (infra/messaging/consumer/)]
  application service 호출
      │
      ▼
[Application Service → Domain → Notifier]
  도메인 처리 후 WebSocket 브로드캐스트
```

### StreamPublisher & Consumer 구조

- 모든 이벤트는 `BaseEvent`를 구현하는 record로 정의한다
- WebSocket Handler(`@MessageMapping`)는 `StreamPublisher.publishAsync(StreamKey, BaseEvent)`로 발행하고, 돌려받은 `CompletableFuture`를 그대로 반환한다. StreamKey로 설정(max-length, thread-pool)을 자동 적용한다 ([ADR-0039](adr/0039-websocket-handler-async-stream-publish.md))
  - XADD 응답을 기다리지 않으므로 Inbound 스레드를 붙잡지 않는다. 클라이언트 응답은 Consumer가 따로 보낸다
  - 발행이 나중에 실패하면 Spring이 반환된 future의 실패를 `@MessageExceptionHandler`로 넘겨 보낸 사용자에게 에러를 보낸다. future를 반환하지 않으면 실패 알림이 사라지므로 ArchUnit `WebSocketHandlerArchitectureTest`가 반환을 강제한다
  - 응답을 기다리는 발행 수는 `redis.stream.publish.max-in-flight`로 제한하고, 넘으면 `STREAM_PUBLISH_OVERLOADED`로 거절한다. 기본값은 10,000이다
  - 종료할 때는 `redis.stream.publish.drain-timeout`만큼 남은 발행이 끝나기를 기다린다. 기본값은 5초다
  - XADD 왕복 시간은 `redis.stream.publish.async.duration`, 응답 대기 수는 `redis.stream.publish.async.in-flight`, 실패와 거절 수는 `redis.stream.publish.async.failed`로 본다
- WebSocket Handler가 아닌 경로는 동기 `StreamPublisher.publish(...)`를 그대로 쓴다. Outbox 릴레이처럼 발행 성공을 확인해야 하는 경로, Application Service, 이벤트 리스너, Consumer가 여기에 해당한다
- Consumer는 `java.util.function.Consumer<구체이벤트타입>`을 구현하고 `@Component` 등으로 스프링 빈으로 등록해야 한다. `EventDispatcher`가 이벤트 타입으로 스프링 컨텍스트에서 빈을 조회하는 구조이므로, 빈 등록이 없으면 소비가 일어나지 않는다. 같은 타입의 Consumer가 여럿이면 전부 호출한다(ADR-0025)
- 순서 보장이 필요한 스트림(카드 선택 등)은 `core-size: 1` 단일 스레드, 브로드캐스트성 스트림은 공용 concurrent 풀을 사용한다

---

## MySQL 처리 흐름

JPA Entity와 도메인 객체를 분리하여 영속성 관심사가 도메인을 오염시키지 않도록 한다.

```text
[Application Service]
      │ 값객체(JoinCode 등)로 조회 요청
      ▼
[QueryService]
  JpaRepository로 조회 → Entity → 도메인 객체 변환
      │
      ▼
[Domain Service]
  순수 도메인 로직 수행
      │
      ▼
[영속성 저장]
  도메인 객체 → Entity 변환 후 JpaRepository.save()
```

복잡한 조회(대시보드, 통계)는 QueryDSL로 타입 안전하게 처리한다.
