# 원초적 규칙 명세서 (R1–R12) — 최소 계약 실험용 계약 문서 (초안)

> 상태: 초안, 팀 검수 대기 (2026-08-28). 실험 설계는 [minimal-contract-experiment.md](minimal-contract-experiment.md).
>
> 이 문서는 두 용도로 쓰인다. ① 최소 계약 실험에서 **구현 에이전트에게 공개되는 계약** — 샌드박스에 이 문서와 게임 동작 스펙만 주입된다. ② 채점 Tier 1의 **규칙 묶음** — 판정 에이전트가 규칙별로 준수 여부를 판정한다. 케이스 체크리스트·가중치는 별도(비공개)다.

## 공통 판정 규약

- 판정값은 규칙별로 `충족` / `위반` / `해당없음` 셋 중 하나다. 부분 점수는 없다 — 한 규칙 안에서 위반 사례가 하나라도 확인되면 `위반`이고, 위반 건수는 근거 목록으로 남긴다.
- 모든 `위반` 판정에는 **근거(`파일:줄`)가 필수**다. 근거 없는 판정은 폐기한다(deep-review 신뢰도 채점과 동일 원칙).
- **근거 검증 랩(환각 방지)** — 근거 자체가 환각일 수 있으므로, 위반 판정은 2단 검증을 통과해야 집계된다: ① 존재 검증(script) — 인용된 파일·줄이 실재하고 인용 문구가 그 지점에 있는지 결정적 검사 ② 지지 검증(저비용 에이전트) — 근거 한 건만 보여주고 "이 코드가 이 주장을 지지하는가: 예/아니오/불명"만 묻는다. 존재 실패·지지 '아니오'는 폐기, '불명'만 판정 에이전트에 재질의.
- 규칙별 검증 주체를 표기한다: `archunit`(기존 ArchUnit 테스트) / `pmd`(기존 PMD 룰셋) / `script`(결정적 스크립트) / `llm`(판정 에이전트). 기계 검증(`archunit`·`pmd`·`script`)이 Tier 0 게이트에서 이미 걸러진 규칙은 Tier 1에서 재판정하지 않는다.
- 판정 기준의 문장은 "~하면 위반" 형식이다. 나열된 기준에 해당하지 않는 회색 지대는 판정 에이전트가 계약 문장의 취지로 판단하되, 확신이 없으면 `충족`으로 둔다(의심만으로 위반 판정 금지).

## 실험 수칙 — 샌드박스 격리 (구현 에이전트 대상)

구현과 탐색은 **체크아웃된 샌드박스 브랜치의 워킹트리 안에서만** 한다. 다음은 금지이며, 시도 자체가 후보 실격 사유다:

- 다른 브랜치·태그·원격 참조 조회 (`origin/*` 참조, `git fetch`/`git pull`, 다른 후보 브랜치 열람)
- 커밋 히스토리 발굴 (`git log`·`git show`로 삭제된 기존 구현을 복원하려는 시도)
- 외부 네트워크에서 기존 구현·답지를 검색하는 행위

샌드박스 작업 공간은 single-branch shallow clone(depth 1)으로 생성되어 위 대부분이 물리적으로 불가능하다 — 이 수칙은 그 강제의 선언이다. 실험의 목적은 계약과 현재 코드베이스만으로 어디까지 해내는지를 재는 것이므로, 답지 접근은 데이터를 무효화한다.

## 제1부 — 패턴 불변 규칙

### R1. 동시성 직렬화

**계약** — 같은 게임 인스턴스(joinCode 단위)의 상태 전이는 어떤 두 실행 경로에서도 동시에 겹치지 않아야 한다. 경합의 원천은 최소 세 가지다: 플레이어 입력끼리, 입력과 타이머 콜백, 서로 다른 서버 인스턴스. 셋 모두에 대해 직렬화 또는 원자성이 보장되어야 한다.

**근거** — `NunchiService` 클래스 주석: "press와 타이머 콜백이 서로 다른 풀에서 같은 게임을 변경하므로 joinCode별 단일 락으로 묶어야 한다 — 컨슈머 단일스레드만으로는 부족하다". 새 패턴에서는 Stream 단일 스레드가 없으므로 이 규칙이 입력 간 경합까지 전담한다. R8은 이 요구를 저장소 수준에서 실현하는 수단이다(R1=요구, R8=수단 — R8을 충족하는 설계는 대체로 R1도 충족한다).

**판정 기준**

- 상태를 읽고 → 검사하고 → 쓰는 경로(check-then-act, read-modify-write)가 락·원자 연산·버전 검사 중 어느 것으로도 보호되지 않으면 위반.
- 타이머 콜백이 입력 처리와 다른 스레드에서 같은 상태를 변경하는데 두 경로가 같은 직렬화 장치를 공유하지 않으면 위반.
- 보호 장치가 JVM 로컬(예: `synchronized`)뿐인데 같은 게임의 명령이 여러 인스턴스에서 처리될 수 있는 설계면 위반(멀티 인스턴스 요구).

**충족 예** — 모든 전이가 Redis Lua 스크립트 하나로 수행됨(읽기·검사·쓰기가 원자). / **위반 예** — `game = repo.find(); if (game.canSelect()) { game.select(); repo.save(game); }`를 아무 보호 없이 실행.

**검증** — `llm` (동시성 테스트가 있으면 참고 근거로 인정).

**해당없음** — 없음(항상 적용).

### R2. 모듈·계층 의존 방향

**계약** — 게임 코드는 다른 게임 패키지·`room`·`user`를 직접 참조하지 않는다. 게임 내부에서 domain은 infra를, application은 ui를 참조하지 않는다. 플레이어 식별은 `Gamer`(:game-api)와 `JoinCode`를 사용한다.

**근거** — `backend/CLAUDE.md` 아키텍처 핵심 제약, ADR-0025, ADR-0034.

**판정 기준** — 기존 ArchUnit 테스트에 전량 위임한다: `GameArchitectureTest`(게임 간 격리, game↛room, game↛user), `GameLayerArchitectureTest`(domain↛infra, application↛ui), `RoomGameSeparationArchitectureTest`(room/game 분리 동결, 화이트리스트 예외 포함). 이 테스트들이 통과하면 충족이다.

**검증** — `archunit` (Tier 0 게이트 — Tier 1 재판정 없음).

**해당없음** — 없음.

### R3. 계층 구조와 네이밍

**계약** — 게임 패키지는 `ui / application / domain / config` 계층으로 구성한다(새 패턴에서 infra 메시징 컨슈머는 없음; 영속·저장소 어댑터를 두면 `infra`). 역할별 클래스 네이밍은 다음을 따른다.

| 역할 | 패턴 | 예시 |
| --- | --- | --- |
| Application Service | `{Domain}Service` | `CardGameService` |
| 플로우 오케스트레이터 | `{Domain}FlowOrchestrator` | `CardGameFlowOrchestrator` |
| WebSocket 알림 | `{Domain}Notifier` | `CardGameNotifier` |
| 도메인 서비스 | `{Domain}CommandService` | `CardGameCommandService` |
| 커맨드 핸들러 | `{Action}CommandHandler` | `SelectCardCommandHandler` |
| 도메인 이벤트 | `{Action}CommandEvent` / `{Domain}Event` | `SelectCardCommandEvent` |
| JPA 엔티티 | `{Domain}Entity` | `RoomEntity` |
| ErrorCode | `{Domain}ErrorCode` | `CardGameErrorCode` |
| 요청 객체 | `{Action}Request` / `{Action}Message` | `RoomEnterRequest` |

WebSocket 메시지를 발행하는 메서드에는 `@WsTopic`(convertAndSend) 또는 `@WsQueue`(convertAndSendToUser)를 반드시 붙인다 — 누락 시 `/dev/ws-catalog`에 컨트랙트가 노출되지 않는다. `@WsTopic.path`는 `/topic/` prefix를 제외한 상대 경로다.

**근거** — `docs/conventions-production.md` L7-33 (기존 표에서 새 패턴에 없는 `{Event}Consumer` 행만 제외).

**판정 기준**

- 위 역할을 수행하는 클래스가 패턴과 다른 이름이면 위반(역할이 없는 클래스는 무관).
- WS 발행 메서드에 `@WsTopic`/`@WsQueue`가 없으면 위반.
- 도메인 로직이 ui/application 계층 클래스 안에 구현되어 있으면 위반(트랜잭션 스크립트 — "비즈니스 로직은 도메인 객체 안에, 서비스는 조합만").

**검증** — `llm` (WS 어노테이션 누락은 `script`로도 가능: 카탈로그 diff가 Tier 0에서 잡는다).

**해당없음** — 해당 역할의 클래스를 만들지 않은 경우 그 행은 판정 제외.

### R4. 이벤트 계약

**계약** — 도메인 이벤트는 record로 정의하고 `BaseEvent`를 구현하며, 컴팩트 생성자에서 `eventId`(UUID)·`timestamp`(`Instant.now()`)를 자동 생성한다. 이벤트에 분산 추적 코드를 넣지 않는다 — **추적 전파는 알림 채널 경계가 담당한다**: 인스턴스 경계를 넘는 알림은 베이스에 제공된 알림 채널 SPI와 traceparent 전파 어댑터(pub/sub·Stream)를 통해 발행하며, 자체 어댑터를 만들 경우에도 전파는 어댑터 경계에서 한다. 이벤트 페이로드에 도메인 객체(엔티티·`CardGame` 등)를 그대로 노출하지 않는다. 같은 JVM 안 모듈 간 이벤트는 동기로 처리한다 — 리스너에 `@Async`를 붙이지 않는다(같은 스레드라 추적도 자동으로 이어진다).

**근거** — `docs/conventions-production.md` L76-82, ADR-0021(트레이싱은 인프라 경계에서 — 기존 `StreamPublisher` inject / Listener extract의 일반화), ADR-0025("`MiniGameFinishedEvent` in-process 동기 리스너 경유, `@Async` 금지").

**판정 기준**

- 이벤트가 record가 아니거나 `BaseEvent` 미구현이면 위반.
- 이벤트 필드에 traceId·span 등 추적 정보가 있으면 위반.
- 이벤트 필드 타입이 도메인 엔티티/애그리거트면 위반(식별자·값·DTO로 풀어야 함).
- 인스턴스 경계를 넘는 알림을 제공된 채널 SPI(또는 경계에서 전파하는 동등한 자체 어댑터) 밖에서 직접 발행하면 위반.
- in-process 이벤트 리스너에 `@Async`가 있으면 위반.

**충족 예** — `record CardSelectedEvent(String joinCode, String playerName, int cardIndex, ...) implements BaseEvent`. / **위반 예** — `record CardSelectedEvent(CardGame game)`.

**검증** — `llm` (record/BaseEvent 여부는 `script` 가능).

**해당없음** — 이벤트를 정의하지 않은 구현(계약 위반이 아니라 §2 패턴 요구 미충족으로 행동 채점에서 걸러짐).

### R5. 서버 권위

**계약** — 시각 판정은 클라이언트가 보낸 값이 아니라 서버 진입점에서 얻은 `Instant.now()`를 쓴다. 플레이어 식별은 클라이언트 페이로드가 아니라 인증된 `Principal`에서 얻는다.

**근거** — ADR-0031 Q1(서버 권위 시각), `NunchiWebSocketController`의 `Principal → PlayerKey.parse(...).playerName()` 선례.

**판정 기준**

- 클라이언트 페이로드의 시각·타임스탬프 필드를 판정(순서·마감 비교 등)에 사용하면 위반.
- 요청 본문의 플레이어 이름/ID를 인증 검증 없이 신뢰해 상태 변경에 사용하면 위반.

**검증** — `llm`.

**해당없음** — 없음.

### R6. 설정 외부화

**계약** — 타이밍·풀 크기 등 조정 가능한 값은 `application.yml`에 선언하고 `@ConfigurationProperties`로 바인딩한다. 코드에 하드코딩하지 않는다.

**근거** — `docs/conventions-production.md` L86-88. 모범 예: `cardgame/config/CardGameTimingProperties.java` — `@Validated @ConfigurationProperties(prefix = "card-game.timing")` record, `Duration` 필드마다 `@NotNull @DurationMin(nanos = 1)`.

**판정 기준**

- 페이즈 지속시간·폴링 주기·리스 TTL·재시도 횟수 같은 튜닝 값이 리터럴로 코드에 박혀 있으면 위반(테스트 코드 제외, 상수화만 하고 yml 미연결이어도 위반).

**검증** — `llm`.

**해당없음** — 조정 가능 값이 없는 구현(현실적으로 없음 — 타이머가 있는 한 항상 적용).

### R7. 테스트 계층 규율

**계약** — 케이스는 가능한 낮은 계층에서 소진한다: 도메인 분기·경계값·상태전이는 순수 Java 단위 테스트(베이스 클래스·Spring 없음), 트랜잭션·조합·이벤트 발행·매핑은 서비스 테스트, 비동기 왕복·실제 영속성·WS 해피패스는 통합 테스트(얇게, 플로우당 현실적 시나리오 1개). 공통 규율: 테스트 메서드명 한글, 복수 검증은 `SoftAssertions`, `Thread.sleep` 금지(비동기 대기는 Awaitility), 테스트 데이터는 픽스처(`*Fixture`/`TestDataHelper`/`*Fake`/`*Dummy`/`Stub*` 5패턴만), 페이즈·타이밍 검증은 실시간 대기가 아니라 즉시 실행 스케줄러로 오케스트레이터 계층에서(`NunchiFlowOrchestratorTest`의 `CapturingScheduler` 참조).

**근거** — `.claude/rules/testing.md`(ADR-0033 피라미드), `testing-domain.md`, `testing-service.md`, `testing-integration.md`, `docs/conventions-test.md`(단일 출처).

**판정 기준**

- 순수 도메인 분기(입력→결과)가 단위 테스트 없이 통합 테스트에서만 검증되면 위반.
- 도메인 단위 테스트가 Spring 컨텍스트·DB·Redis를 요구하면 위반.
- `Thread.sleep`으로 비동기·타이밍을 검증하면 위반(PMD `NoThreadSleep`이 게이트에서도 잡음).
- 테스트 데이터 생성 클래스가 5패턴 외 이름이면 위반.
- 타이머·페이즈 전이를 실시간 흐름으로만 검증하고 스케줄러 치환 테스트가 없으면 위반.

**검증** — `pmd`(sleep·JUnit 단언) + `llm`(계층 배분·픽스처).

**해당없음** — 없음(자작 테스트는 게이트 필수이므로 항상 존재).

## 제2부 — 외부 저장이 신설하는 규칙

### R8. 원자적 상태 전이

**계약** — 게임 상태의 전이는 저장소에서 원자적으로 수행한다. "읽어서 → 검사하고 → 쓰는" 사이에 다른 전이가 끼어들 수 없어야 한다. 수단은 자유다(Redis Lua 스크립트, WATCH/MULTI, 버전 필드 CAS, DB 비관/낙관 락 등) — 부재만 위반이다.

**근거** — 신설. 현행 구조에서 Stream 단일 스레드 + JVM 락이 하던 직렬화를 저장소가 승계한다(설계 문서 §2).

**판정 기준**

- 저장소에서 읽은 상태를 애플리케이션 메모리에서 수정한 뒤 무조건 덮어쓰면(lost update 가능) 위반.
- 검사와 쓰기가 별개 왕복인데 그 사이의 경합을 감지·거부하는 장치(버전 불일치 거부 등)가 없으면 위반.
- 거부당한 전이를 그대로 삼키면(재시도도 거절 응답도 없음) 위반 — 충돌은 해소되어야 하지 무시되면 안 된다.

**충족 예** — JPA `@Version` 낙관 락 / Redis 버전 CAS(`WATCH state:{joinCode}` 후 MULTI, 또는 "버전 일치 시 값+버전 교체" 제네릭 Lua 1개) / `@RedisLock` 안에서 read-modify-write — 셋 다 충족이다. / **위반 예** — `redis.set(key, serialize(game))`로 무조건 덮어쓰기.

> **주의 — 로직의 Lua 이전 함정**: 도메인 검증 로직(선택 가능 여부, 라운드 전이 조건 등) 자체를 저장소 스크립트(Lua 등)로 옮기면 R7의 도메인 순수 Java 단위 테스트가 불가능해진다. 원자 교환 프리미티브는 저장소에, 게임 규칙은 Java 도메인에 둔다.

**검증** — `llm` (경합 테스트가 있으면 근거로 인정).

**해당없음** — 없음.

### R9. 저장 성공 후 발행

**계약** — 이벤트·알림은 상태 저장이 성공한 뒤에만 발행한다. 저장 실패 시 이벤트가 나가면 안 된다.

**근거** — 신설. 순서가 뒤집히면 구독자가 존재하지 않는 상태를 본다. R12와 짝이다 — R9가 발행 측, R12가 수신 측의 규율.

**판정 기준**

- 코드 순서상 저장 호출 전에 발행이 있으면 위반.
- 저장이 실패(예외·CAS 거부)해도 발행이 실행되는 경로가 있으면 위반.
- DB 트랜잭션 안에서 발행하는 경우, 커밋 전 발행이 롤백과 함께 취소되지 않는 구조면 위반(`@TransactionalEventListener(AFTER_COMMIT)` 등으로 해소 가능).

**검증** — `llm`.

**해당없음** — 없음.

### R10. 핸들러 멱등성

**계약** — 같은 명령이 중복 도착해도(클라이언트 재전송, 재시도) 상태가 이중 전이되지 않아야 한다.

**근거** — 신설. Stream 제거로 전달 보장 계층이 사라지므로 처리 계층이 중복을 흡수해야 한다.

**판정 기준**

- 같은 명령을 두 번 처리했을 때 결과가 달라지는 전이(카운터 증가, 카드 중복 선택 허용 등)에 중복 방지 장치가 없으면 위반.
- 상태 기반 거부(예: "이미 이 라운드에 선택함 → 거부")가 있으면 충족으로 인정 — 별도 dedup 키가 필수인 것은 아니다.

**검증** — `llm` (중복 입력 테스트가 있으면 근거로 인정).

**해당없음** — 자연 멱등인 명령(같은 값으로 덮어쓰기만 하는 조회성 명령)뿐인 경우.

### R11. 핫패스 지연 예산

**계약** — 게임 진행 중 입력·타이머 처리의 동기 경로는 **상태 저장소 왕복 1회 수준**으로 유지한다. 게임 이력·정산성 영속(결과 저장, 통계)은 게임 루프 밖(종료 후 이벤트 경로)에서만 한다. 상태 저장소가 무엇인지(Redis/MySQL)는 규정하지 않는다 — 관점 축의 실험 대상이다.

**근거** — 신설. 카드 선택은 초 단위 응답성이 필요하고, 결과 영속은 현행도 게임 종료 후 이벤트 경로에서 수행한다(`MiniGameResultSaveEventListener`).

**판정 기준**

- 입력·타이머 처리의 동기 경로에 상태 저장소 외의 추가 동기 I/O(별도 시스템 조회, 이력 테이블 기록 등)가 있으면 위반.
- 한 명령 처리에 상태 저장소 왕복이 반복 누적되는 구조(N+1 조회 등)면 위반.
- 상태 저장소가 MySQL인 설계 자체는 위반이 아니다.
- 게임 종료·정산 시점의 DB 영속은 충족(위반 아님).

**검증** — `llm` (`script` 보조 가능: 핫패스 패키지의 의존 grep).

**해당없음** — 없음.

### R12. 알림은 진실이 아니다 — 스냅샷 발행과 재동기화

**계약** — 상태 알림 페이로드는 발행 직전 저장소에서 읽은 **최신 전체 스냅샷**으로 구성한다(전이 시점의 메모리 객체 재사용 금지). 발행 실패는 게임 상태에 영향을 주지 않으며, 다음 발행이 자연 치유한다. 클라이언트 재연결·재구독 시 현재 스냅샷을 받아볼 수 있는 경로를 기존 컨트랙트 범위 안에서 제공한다. 단, **컨트랙트는 동결이다** — 기존 토픽 경로·페이로드 타입에 필드를 추가·변경할 수 없다(부록 B). 내부 상태(저장소 스키마)는 자유이므로 페이즈 마감 시각 등은 내부에만 둔다.

**근거** — 설계 문서 §2 정합성 모델. 스냅샷 발행 선례: `CardGameNotifier`가 매 변경마다 `MiniGameStateMessage.from(cardGame)` 전체 상태를 발행. 마감 시각을 상태에 싣는 선례: `NunchiFlowOrchestrator`의 `idleDeadlineEpochMs` + `serverNowEpochMs`(단, 카드게임 컨트랙트에는 해당 필드가 없으므로 페이로드 추가는 불가).

**판정 기준**

- 알림 페이로드를 저장소 재조회 없이 전이 시점의 메모리 객체로 만들면 위반(자기 인스턴스의 낡은 뷰를 발행할 수 있음).
- 알림을 델타(변경분)로만 발행해 유실 시 다음 메시지로 복구가 불가능하면 위반.
- 발행 실패가 상태 전이를 롤백하거나 예외로 전이 자체를 실패시키면 위반(알림은 부수 효과다).
- 재연결한 클라이언트가 현재 상태를 받을 방법이 없으면 위반.
- 기존 페이로드 record에 필드를 추가·삭제·개명하면 위반(Tier 0 컨트랙트 diff에서도 잡힘).

**검증** — `llm` + `script`(컨트랙트 diff는 Tier 0).

**해당없음** — 없음.

## 부록 A — 관찰 항목 (계약 미포함, 측정 전용)

아래 항목은 **에이전트에게 주는 계약에 포함하지 않는다.** 기존 컨벤션 문서에 있는 규칙들로, "계약에 없어도 지키는가"를 관찰해 문서 다이어트 4분면의 데이터로 쓴다. Tier 1 점수에 넣지 않고 별도 리포트로만 집계한다.

| 관찰 항목 | 출처 | 판정 |
| --- | --- | --- |
| 도메인 예외는 `BusinessException(${Domain}ErrorCode, 메시지)` — JDK 표준 예외 직접 사용 금지 | rules/production.md | llm |
| 예외 메시지 한국어 | rules/production.md | llm |
| 도메인 식별자·핵심 개념은 record 값 객체, 원시 타입 시그니처 노출 금지 | conventions-production.md L35-37 | llm |
| 도메인 계층은 Request/Response DTO에 의존하지 않음 | rules/production.md | llm |
| 시간 필드는 `LocalDateTime` 대신 `Instant` | rules/production.md | llm |
| 상태 변경 메서드는 결과를 반환 | conventions-production.md | llm |
| 사이드 이펙트와 계산 로직 분리, 외부 의존성(시간·랜덤) 파라미터 주입 | conventions-production.md | llm |
| early return·중첩 깊이·필드 주입 금지·`System.out` 금지 등 | PMD 룰셋 | 게이트 소관(관찰 불필요 — CI가 잡음) |

## 부록 B — 컨트랙트 기준면 (Tier 0 diff 기준, 동결)

재구현은 아래 컨트랙트를 경로·페이로드 타입까지 그대로 유지해야 한다. FE는 수정하지 않는다.

**인바운드 (WS)** — `MiniGameWebSocketController`

- `@MessageMapping("/room/{joinCode}/minigame/command")` — 공용 단일 진입점, `@WsReceive(respondsOnTopics = {"/room/{joinCode}/round", "/room/{joinCode}/gameState"})`. 카드게임 명령은 `SelectCardCommand`로 디스패치된다.

**아웃바운드 (WS 토픽)** — `CardGameNotifier` 기준

| 토픽 | 페이로드 | 트리거 |
| --- | --- | --- |
| `/topic/room/{joinCode}/gameState` | `MiniGameStateMessage` | 카드 선택, 단계 완료 |
| `/topic/room/{joinCode}/round` | `MiniGameStartMessage` | 게임 시작 |

**REST** — `MiniGameRestController` (ADR-0025 결정 7: 클라이언트 호환을 위해 URL 유지)

- `GET /minigames/scores?joinCode=&miniGameType=`
- `GET /minigames/ranks?joinCode=&miniGameType=`
- `GET /rooms/minigames`
- `GET /rooms/minigames/selected?joinCode=`
- `GET /rooms/{joinCode}/miniGames/remaining`

Tier 0 게이트는 재구현의 `@WsTopic`/`@WsReceive` 카탈로그와 REST 매핑을 위 기준면과 diff하여, 경로·페이로드 타입이 하나라도 다르면 탈락시킨다.

---

검수 포인트(팀): ① R1/R8 경계가 명확한지 ② R10의 "상태 기반 거부 인정"이 충분한지 ③ R12의 컨트랙트 동결 아래에서 재연결 재동기화가 실제로 가능한지(재구독 시 `gameState` 재발행이 기존 FE 동작과 호환되는지) ④ 부록 A 항목 추가·제외.
