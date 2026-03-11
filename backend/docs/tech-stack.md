# 기술 스택 & 처리 흐름

## 기술 스택

| 분류         | 기술                                            |
|------------|-----------------------------------------------|
| 프레임워크      | Spring Boot 3.5.3, Java 21                    |
| 실시간 통신     | WebSocket (STOMP) + SockJS                    |
| 메시징        | Redis Stream                                  |
| 캐시 / 분산 락  | Valkey(Redis) + Redisson                      |
| 영속성        | Spring Data JPA + QueryDSL, MySQL 8.0         |
| DB 마이그레이션  | Flyway                                        |
| 서킷 브레이커    | Resilience4j                                  |
| 관찰 가능성     | Micrometer + Prometheus, OpenTelemetry(Tempo) |
| 외부 스토리지    | Oracle Object Storage (QR 코드)                 |
| 테스트        | JUnit 5, AssertJ, TestContainers              |

---

## Redis Stream 처리 흐름

```
[클라이언트 WebSocket]
      │ 메시지 전송 (/app/...)
      ▼
[ui/ Handler]
  커맨드를 받아 BaseEvent 구현체(record) 생성
      │
      ▼
[StreamPublisher.publish(StreamKey, BaseEvent)]
  JSON 직렬화 후 Redis Stream에 XADD
      │
      ▼
[Redis Stream (Valkey)]
  StreamKey 별로 독립된 스트림으로 적재
      │
      ▼
[RedisStreamListenerStarter]
  StreamKey별 StreamMessageListenerContainer + 전용 ThreadPool로 폴링
      │
      ▼
[EventDispatcher.handle(BaseEvent)]
  1. JSON 역직렬화 → @JsonTypeInfo로 구체 타입 복원
  2. ResolvableType으로 Consumer<이벤트타입> 빈 동적 조회
  3. Traceable이면 트레이스 컨텍스트 주입 후 실행
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
- `StreamPublisher.publish(StreamKey, BaseEvent)`로 발행한다. StreamKey로 설정(max-length, thread-pool)을 자동 적용한다
- Consumer는 `java.util.function.Consumer<구체이벤트타입>`을 구현하는 것만으로 `EventDispatcher`에 자동 연결된다. 별도 등록 코드가 필요없다
- 순서 보장이 필요한 스트림(카드 선택 등)은 `core-size: 1` 단일 스레드, 브로드캐스트성 스트림은 공용 concurrent 풀을 사용한다

---

## MySQL 처리 흐름

JPA Entity와 도메인 객체를 분리하여 영속성 관심사가 도메인을 오염시키지 않도록 한다.

```
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
