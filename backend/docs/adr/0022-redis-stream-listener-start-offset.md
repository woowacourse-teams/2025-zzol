# ADR-0022: Redis Stream 리스너 시작 오프셋 — fromStart 리플레이 제거와 구독 활성화 대기

## 상태

승인 (2026-06-07)

## 컨텍스트

`RedisStreamListenerStarter`는 컨슈머 그룹 없이(standalone read) `StreamOffset.fromStart()`(0-0)로 모든 스트림을 구독한다. 이 구성에 두 가지 문제가 있다.

1. **재시작 리플레이**: 0-0부터 읽으므로 앱 재시작마다 스트림에 잔존하는 메시지 전체를 재처리한다. 브로드캐스트 이벤트가 재시작 직후 클라이언트에 중복 전송될 수 있다.
2. **구독 활성화 시점 미보장**: `container.register()`가 반환하는 `Subscription`을 버리고 있어, "폴링이 실제로 시작됐는지"를 프로덕션·테스트 어디서도 확인할 수 없다. `fromStart`는 놓친 메시지를 과거분 재독으로 가려주기 때문에 이 레이스가 드러나지 않았을 뿐이다. 과거에 `latest()`로 전환을 시도했을 때 테스트가 간헐 실패한 근본 원인이 이것이다.

부수적으로 errorHandler가 container options와 read request 양쪽에 중복 등록되어 있다.

### 라이브러리 동작 조사 (spring-data-redis 3.5.1 소스 근거)

| 시작 오프셋                        | `ReadOffsetStrategy` | 다음 폴링 오프셋      | 결과                      |
|-------------------------------|----------------------|----------------|-------------------------|
| `fromStart()` (0-0)           | NextMessage          | 마지막 수신 ID      | 무손실, 단 재시작 리플레이         |
| `latest()` (`$`)              | Latest               | **항상 `$` 재사용** | **폴링 사이 메시지 상시 유실**     |
| `lastConsumed()` (standalone) | LastConsumed         | 마지막 수신 ID      | 첫 메시지 수신 전까지 `$` 레이스 잔존 |
| 구체 ID `from(id)`              | NextMessage          | 마지막 수신 ID      | 무손실, 리플레이 없음            |

- `Latest` 전략은 `getNext()`도 `$`를 반환한다. 즉 `StreamOffset.latest()`는 첫 폴링뿐 아니라 **매 폴링마다** "이 XREAD 이후" 메시지만 읽는다 — 두 XREAD 사이에 발행된 메시지는 운영 중에도 영구 유실된다. 단순 `latest()` 전환이 답이 될 수 없는 이유다
- `StreamPollTask.PollState`는 container `stop()`/`start()` 사이에 `currentOffset`을 유지한다 — `RedisStreamContainerRecovery`가 재시작해도 끊긴 지점부터 재개된다 (NextMessage 전략일 때)
- `StreamPollTask`는 read request의 errorHandler를 우선 사용하고, container options의 errorHandler는 request에 없을 때의 폴백일 뿐이다

## 결정

### 1. 시작 오프셋: 기동 시점 마지막 ID 해석

구독 등록 시점에 `XREVRANGE <key> + - COUNT 1`로 스트림의 마지막 레코드 ID를 조회해 `ReadOffset.from(lastId)`로 시작한다. 스트림이 비었거나 없으면 `0-0`을 사용한다.

- 기동 이전 메시지는 건너뛴다 (리플레이 제거)
- 기동 이후 메시지는 빠짐없이 소비한다 (NextMessage 전략)
- **오프셋이 등록 시점에 고정되므로**, 첫 XREAD가 실행되기 전에 발행된 메시지도 수신된다 — `$`의 활성화 레이스가 원천 제거된다 (테스트 결정성 확보)

### 2. 구독 등록 순서: register → start → await

```java
final var container = createContainer(...);          // start() 호출 제거
final Subscription subscription = container.register(buildReadRequest(streamKey), this::onMessage);
container.start();
// 타임아웃은 commonSettings.subscriptionStartTimeout(기본 5s)으로 설정화
subscription.await(properties.commonSettings().subscriptionStartTimeout());
```

`await()` 실패(타임아웃·인터럽트)는 `IllegalStateException`으로 전환해 **앱 기동을 실패시킨다**. 메시지 처리 흐름 전체가 Redis Stream을 경유하므로(CLAUDE.md 핵심 제약) 구독 없는 기동은 무의미하다 — fail-fast 후 오케스트레이터 재시작이 옳다.

대기 한도는 하드코딩하지 않고 `common-settings.subscription-start-timeout`(`@DefaultValue("5s")`)으로 바인딩한다 — 환경별로 조정 가능하다.

### 3. errorHandler 단일화

container options의 errorHandler를 제거한다. 진실 공급원은 `buildReadRequest()` 하나다. options 쪽을 폴백으로 남기면 "errorHandler는 있는데 `cancelOnError`는 기본값(`t -> true`)인 구독"이 조용히 생길 수 있다 — 예외 1건으로 구독이 영구 취소되는, 직전에 수정한 버그(PR #1359)의 재발 경로다. 모든 구독 등록은 `buildReadRequest()`를 경유해야 한다.

### 4. 컨테이너 장부 단일화: `RedisStreamContainerRegistry`

기존에는 같은 컨테이너가 두 곳에 기록됐다 — Spring 컨텍스트에 동적 빈으로(`registerBean` + `stream-container-%s` 문자열 빈 이름) 등록하고, 종료용으로 별도 List에도 담았다. 이를 스트림 키별 단일 장부(`ConcurrentHashMap`)인 `RedisStreamContainerRegistry`로 일원화한다.

- 등록은 Starter가 전담한다 (`registry.register()` — start/await **이전**에 호출하므로 await 실패로 기동이 중단돼도 stop 대상에 포함된다)
- 조회는 Recovery·HealthIndicator가 `registry.find()`(Optional)로 한다 — 기존 `applicationContext.getBean(문자열, ...)` + `NoSuchBeanDefinitionException` catch 방식을 폐기한다
- 동적 빈 등록·문자열 빈 이름 결합(프로덕션 + 테스트)이 완전히 제거된다

## 구현 시 주의사항

### 공유 스레드풀 사이징 — core-size ≥ 스트림 수

폴링 태스크는 무한 루프로 **스레드를 영구 점유**한다. `ThreadPoolExecutor`는 큐가 가득 차야 core 초과 스레드를 증설하므로, 공유 풀의 core-size가 그 풀을 쓰는 스트림 수보다 작으면 초과분 스트림의 폴링은 큐에서 영원히 시작되지 않는다.

- 기존(`fromStart` + await 없음)에는 이 상태가 **침묵**했다 — 구독이 안 시작돼도 기동은 성공했다
- 이제 `await()`가 기동 시점에 fail-fast로 잡는다. 실제로 `application-test-base.yml`의 `concurrent` 풀(core 2, 사용 스트림 7개)이 이 검증에 걸려 core 8로 수정했다 (운영 `redis.yml`은 core 8로 정상)

### refresh 실패 시 고아 컨테이너

`ContextClosedEvent`는 **refresh 실패 시 발행되지 않는다**. `@PostConstruct` 도중 예외가 나면 이미 start된 컨테이너가 stopping=false 상태로 남아, 파괴된 커넥션 팩토리에 무한 폴링하며 ERROR를 쏟는다. 봉합 책임은 둘로 나뉜다:

- `RedisStreamContainerRegistry`(스트림 키별 컨테이너 단일 장부)가 `@PreDestroy`(정상 종료·refresh 실패 양쪽에서 호출됨)에서 일괄 `stop()`한다. Starter는 start/await **이전**에 레지스트리에 등록하므로 기동 중단 시에도 stop 대상에 포함된다
- Starter의 `@PreDestroy`는 stopping 플래그만 확정한다. **Starter가 레지스트리를 생성자 주입하므로 빈 파괴는 역의존 순서(Starter → Registry)** — 플래그 설정이 일괄 stop보다 항상 먼저 실행된다. 이 불변식은 생성자 주입 관계에 의존하므로, 주입 방식을 setter/lazy로 바꾸면 깨진다

## 트레이드오프

- **Redis 다운 시 기동 실패**: 기존에는 Redis가 죽어 있어도 앱은 떴다(폴링 에러만 반복). 이제 오프셋 해석(XREVRANGE)이 실패하면 기동이 실패한다. Stream이 메시지 흐름의 필수 경로인 이상 의도된 동작이다
- **기동 시 스트림당 XREVRANGE 1회**: COUNT 1 조회라 비용은 무시할 수준
- **재시작 직전 발행분 유실 가능**: 인스턴스가 죽어 있는 동안 발행된 메시지는 새 인스턴스가 건너뛴다. 컨슈머 그룹이 없는 현 구조에서 이미 동일했던 제약이며(처리 보장은 Outbox 경로가 담당), 브로드캐스트 이벤트는 일시 유실 허용이다

## 고려한 대안

### 대안 A: fromStart 유지 (현행)

- 장점: 무손실, 변경 없음
- 단점: 재시작마다 전체 리플레이 — 중복 브로드캐스트, 트림 정책에 결합된 기동 비용
- 기각: 리플레이는 가려진 버그이지 기능이 아니다

### 대안 B: `StreamOffset.latest()`

- 장점: 코드 한 줄, 리플레이 제거
- 단점: `Latest` 전략이 매 폴링 `$`를 재사용 — 폴링 사이 발행분 상시 유실
- 기각: 운영 중 메시지 유실은 수용 불가

### 대안 C: `ReadOffset.lastConsumed()` (standalone)

- 장점: 첫 수신 이후로는 무손실, 추가 Redis 호출 없음
- 단점: 첫 메시지 수신 전까지 `$` 단계가 남아 활성화 레이스 잔존 — 테스트가 검증하려는 첫 메시지가 정확히 레이스 대상
- 기각: 핵심 문제(결정성)를 해결하지 못함

### 대안 D: 컨슈머 그룹 도입

- 장점: ACK 기반 처리 보장, 다운타임 발행분 재전달
- 단점: 그룹 내 메시지 분배는 브로드캐스트 의미론과 충돌 — 인스턴스별 고유 그룹 + PEL 관리·고아 그룹 정리 등 운영 복잡도가 크게 증가
- 기각: 현 요구(브로드캐스트, 일시 유실 허용) 대비 과도. 처리 보장이 필요한 경로는 이미 Outbox가 담당

## 결과

- 재시작 시 과거 이벤트 리플레이가 사라진다
- `@PostConstruct` 완료 = 전 스트림 폴링 활성화가 보장된다 — 통합 테스트는 컨텍스트 기동 후 발행하면 결정적으로 수신한다
- `RedisStreamContainerRecovery` 재시작 경로는 PollState 오프셋 유지로 끊긴 지점부터 재개된다
- `RedisStreamLagMetricService`의 fromStart 전제 주석 갱신 필요
