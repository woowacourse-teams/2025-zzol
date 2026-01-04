# Coffee Shout 백엔드 프로젝트

## 프로젝트 개요
- **프로젝트명**: Coffee Shout (커피 내기 게임 플랫폼)
- **언어**: Java 21
- **프레임워크**: Spring Boot 3.5.3
- **빌드 도구**: Gradle (Kotlin DSL)

## 기술 스택
- **데이터베이스**: MySQL (프로덕션), H2 (테스트)
- **캐시/메시징**: Redis (Redisson 3.27.2)
- **ORM**: JPA + QueryDSL 5.0.0
- **실시간 통신**: WebSocket
- **클라우드**: Oracle Cloud Infrastructure (Object Storage)
- **문서화**: SpringDoc OpenAPI 2.8.3
- **모니터링**: Micrometer + Prometheus + OpenTelemetry

## 아키텍처

### 레이어 구조
프로젝트는 **레이어드 아키텍처**와 **DDD(Domain-Driven Design)** 패턴을 따릅니다.

```
src/main/java/coffeeshout/
├── domain/          # 도메인 모델, 엔티티, 이벤트, 리포지토리 인터페이스
├── application/     # 비즈니스 로직, 서비스 레이어
├── infra/          # 인프라 구현 (Redis, 메시징, 외부 API)
└── ui/             # 컨트롤러, 요청/응답 DTO
```

### 도메인 모듈
- `room`: 방 생성, 입장, 관리
- `cardgame`: 카드 게임 로직
- `racinggame`: 레이싱 게임 로직
- `minigame`: 미니게임 로직
- `dashboard`: 대시보드 기능
- `global`: 공통 기능 (Redis, 설정, 유틸리티)

### 이벤트 기반 아키텍처
**Redis Stream**을 활용한 이벤트 기반 메시징 패턴 사용:

```
Event 발행 → StreamPublisher → Redis Stream → Consumer → 비즈니스 로직 처리
```

**주요 컴포넌트**:
- `BaseEvent`: 모든 이벤트의 기본 인터페이스
- `StreamPublisher`: Redis Stream에 이벤트 발행
- `StreamKey`: Stream 채널 정의 (예: `ROOM_BROADCAST`)
- `*Consumer`: 이벤트를 소비하고 비즈니스 로직 실행

**예시**:
```java
// Event 정의
public record RoomCreateEvent(
    String eventId,
    TraceInfo traceInfo,
    Instant timestamp,
    String hostName,
    String joinCode
) implements BaseEvent, Traceable { }

// Consumer 구현
@Component
public class RoomCreateConsumer implements Consumer<RoomCreateEvent> {
    @Override
    public void accept(RoomCreateEvent event) {
        roomEventService.createRoom(event);
    }
}

// Event 발행
streamPublisher.publish(StreamKey.ROOM_BROADCAST, new RoomCreateEvent(hostName, joinCode));
```

## 테스트 전략

### 테스트 타입
1. **IntegrationTest** (`@IntegrationTest`)
   - 전체 스택 통합 테스트
   - TestContainers 사용 (Redis, MySQL)
   - WebSocket 통합 포함
   - 실제 외부 시스템과의 상호작용 검증

2. **ServiceTest** (`extends ServiceTest`)
   - 서비스 레이어 단위 테스트
   - ApplicationEventPublisher 모킹
   - 트랜잭션 롤백 (@Transactional)

### 테스트 네이밍 컨벤션
**한글 메서드명 사용** (가독성 우선):
```java
@Test
void 방_생성_처리_시_실제_Room_엔티티가_올바르게_생성된다() {
    // given
    // when
    // then
}

@Nested
class 실제_비즈니스_로직_테스트 {
    // 테스트 메서드들
}
```

### 테스트 메서드 구조
**Given-When-Then 패턴**을 주석으로 명시:

```java
@Test
void 사용자를_생성한다() {
    // given
    String userName = "테스트유저";

    // when
    User user = userService.create(userName);

    // then
    assertThat(user.getName()).isEqualTo(userName);
}

@Test
void 사용자_삭제_요청시_존재하지_않는다면_예외가_발생한다() {
    // given
    Long nonExistentId = 999L;

    // when & then
    assertThatThrownBy(() -> userService.delete(nonExistentId))
        .isInstanceOf(UserNotFoundException.class);
}
```

**주의사항**:
- 각 섹션(given/when/then)은 주석으로 명확히 구분
- when과 then이 함께 있는 경우 `// when & then` 사용
- 가독성을 위해 섹션 사이 빈 줄 추가

### Fixture 패턴
테스트 데이터는 **Fixture 클래스**로 관리:
- 위치: `src/test/java/coffeeshout/fixture/`
- 예시: `RoomFixture`, `PlayerFixture`, `PlayersFixture`
- 한글 메서드명으로 의미 명확화

```java
public final class RoomFixture {
    public static Room 호스트_꾹이() {
        final Room room = new Room(
            new JoinCode("A4BX"),
            PlayerFixture.호스트꾹이().getName()
        );
        return room;
    }
}
```

### 비동기 테스트
**Awaitility** 사용:
```java
await().atMost(Duration.ofSeconds(5))
    .pollInterval(Duration.ofMillis(100))
    .untilAsserted(() -> {
        Room createdRoom = roomRepository.findByJoinCode(joinCode).orElseThrow();
        assertThat(createdRoom).isNotNull();
    });
```

### Stream Producer 테스트 패턴
Redis Stream을 통한 이벤트 처리를 검증하는 통합 테스트:

```java
@IntegrationTest
class RoomCreateStreamProducerTest {
    @Autowired RoomRepository roomRepository;
    @Autowired StreamPublisher streamPublisher;

    @Test
    void 방_생성_처리_시_실제_Room_엔티티가_올바르게_생성된다() {
        // given
        String hostName = "호스트";
        String joinCode = "ABCD1234";
        RoomCreateEvent event = new RoomCreateEvent(hostName, joinCode);

        // when
        streamPublisher.publish(StreamKey.ROOM_BROADCAST, event);

        // then
        await().atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                Room createdRoom = roomRepository.findByJoinCode(new JoinCode(joinCode))
                    .orElseThrow(() -> new IllegalStateException("방이 생성되지 않음"));
                assertThat(createdRoom.getJoinCode().getValue()).isEqualTo(joinCode);
            });
    }
}
```

**핵심 포인트**:
- StreamPublisher로 이벤트 발행
- await()로 비동기 처리 대기
- Repository로 실제 데이터 변경 검증
- 멱등성, 동시성, JSON 직렬화 검증 테스트 포함

## 코딩 컨벤션

### 일반 규칙
- **Java 21** 기능 적극 활용 (Record, Pattern Matching, Virtual Threads 등)
- **Lombok** 사용: `@RequiredArgsConstructor`, `@Getter` 등
- **불변성 우선**: Record, final 키워드 활용
- **한글 식별자**: 테스트 메서드, Fixture 메서드는 한글 사용 가능

### 네이밍 컨벤션
- **Event**: `*Event` (예: `RoomCreateEvent`, `RoomJoinEvent`)
- **Consumer**: `*Consumer` (예: `RoomCreateConsumer`)
- **Service**: `*Service` (예: `RoomEventService`, `RoomQueryService`)
- **Repository**: `*Repository` (예: `RoomRepository`)
- **Fixture**: `*Fixture` (예: `RoomFixture`)
- **Test**: `*Test`, `*StreamProducerTest`, `*ConsumerTest`

### Value Object 패턴
도메인 값은 Value Object로 래핑:
```java
public record JoinCode(String value) {
    public JoinCode {
        // 검증 로직
    }
}
```

### JoinCode 생성 규칙

**JoinCode는 반드시 `JoinCodeGenerator`를 통해 생성해야 합니다.**

#### 생성 방법
```java
@Autowired
private JoinCodeGenerator joinCodeGenerator;

// JoinCode 생성 (중복 체크 포함)
JoinCode joinCode = joinCodeGenerator.generate();
```

#### JoinCode 특징
- **길이**: 정확히 4글자
- **사용 가능한 문자**: `ABCDFGHJKLMNPQRSTUVWXYZ346789` (29자)
- **제외된 문자** (혼동 방지):
  - `E` (숫자 `3`과 혼동)
  - `I` (숫자 `1`과 혼동)
  - `O` (숫자 `0`과 혼동)
  - `0`, `1`, `2`, `5` (문자와 혼동)

#### 생성 프로세스
1. `JoinCode.generate()`: 랜덤으로 4글자 코드 생성
2. `JoinCodeGenerator.generate()`:
   - Redis에 중복 체크
   - 중복 시 최대 100번까지 재시도
   - 성공 시 Micrometer로 메트릭 기록
   - 실패 시 `IllegalStateException` 발생

#### 테스트 시 주의사항
```java
// ✅ 올바른 테스트 코드 (Generator 사용)
JoinCode joinCode = joinCodeGenerator.generate();

// ✅ Fixture에서 하드코딩 (테스트용)
JoinCode joinCode = new JoinCode("A4BX");  // 유효한 문자만 사용

// ❌ 잘못된 예시
JoinCode joinCode = new JoinCode("ABCD1234");  // 4글자 초과
JoinCode joinCode = new JoinCode("A0I5");      // 금지된 문자 사용
```

### Event 구조
모든 이벤트는 BaseEvent 구현:
```java
public record SomeEvent(
    String eventId,          // UUID
    TraceInfo traceInfo,     // 분산 추적 정보
    Instant timestamp,       // 이벤트 발생 시간
    // ... 도메인 데이터
) implements BaseEvent, Traceable {

    // 편의 생성자
    public SomeEvent(String domainData) {
        this(
            UUID.randomUUID().toString(),
            TraceInfoExtractor.extract(),
            Instant.now(),
            domainData
        );
    }
}
```

## 자주 사용하는 명령어

### 빌드 및 실행
```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "RoomCreateStreamProducerTest"

# 애플리케이션 실행 (Docker Compose 포함)
./gradlew bootRun
```

### Git 관련
- **메인 브랜치**: `main`
- **개발 브랜치**: `be/dev`, `fe/dev`
- **커밋 메시지**: `[타입] 설명` 형식 (예: `[feat]`, `[refactor]`, `[fix]`)

## 주의사항

### Redis Stream 이벤트 처리
1. **멱등성 보장**: 중복 이벤트 처리를 고려한 로직 작성
2. **비동기 처리**: Consumer는 별도 스레드에서 실행됨
3. **에러 처리**: Consumer에서 예외 발생 시 재처리 로직 고려

### 테스트 작성 시
1. **Fixture 재사용**: 기존 Fixture 먼저 확인, 필요시 확장
2. **비동기 검증**: Awaitility 사용, 적절한 타임아웃 설정
3. **트랜잭션 격리**: 각 테스트는 독립적으로 실행되어야 함
4. **TestContainers**: 실제 Redis, MySQL 환경에서 테스트

### 코드 품질
1. **불변성**: 가능한 모든 객체를 불변으로 설계
2. **단일 책임**: 각 클래스/메서드는 하나의 책임만
3. **명시적 검증**: Value Object로 도메인 규칙 명시
4. **이벤트 추적**: TraceInfo를 통한 분산 추적 활용

## 새로운 기능 추가 시 체크리스트

### 새로운 이벤트 추가
- [ ] `domain/event/`에 Event 클래스 생성 (BaseEvent 구현)
- [ ] `infra/messaging/consumer/`에 Consumer 구현
- [ ] `application/service/`에 비즈니스 로직 구현
- [ ] StreamKey에 새 채널 추가 (필요시)
- [ ] `*StreamProducerTest` 작성 (통합 테스트)
- [ ] `*ConsumerTest` 작성 (단위 테스트)

### 새로운 도메인 추가
- [ ] `domain/` 디렉토리 구조 생성
- [ ] Entity, Value Object 정의
- [ ] Repository 인터페이스 정의
- [ ] Service 레이어 구현
- [ ] Controller (필요시)
- [ ] Fixture 클래스 작성
- [ ] 테스트 작성

## 프롬프팅 가이드

이 프로젝트에서 Claude Code에 요청할 때 유용한 프롬프트:

### 테스트 작성
```
RoomEnterStreamProducerTest를 기반으로 [기능명]StreamProducerTest를 작성해줘.
실제로 [기대 동작]이 되는지 확인하면 돼.
```

### 이벤트 추가
```
[도메인]에 [이벤트명]Event를 추가해줘.
- 필요한 데이터: [데이터 목록]
- Consumer에서는 [처리 내용]을 수행해야 해
- 기존 [유사 이벤트]를 참고해서 패턴을 맞춰줘
```

### 코드 리팩토링
```
[파일명]을 프로젝트 컨벤션에 맞게 리팩토링해줘.
- Record 패턴 활용
- Value Object로 도메인 값 래핑
- 한글 테스트 메서드명 사용
```

### 문제 해결
```
[테스트/기능]이 실패해.
로그: [에러 로그]
기존 [유사 기능]은 정상 동작하는데 뭐가 문제일까?
```