# 0010. StompSessionManager — 인메모리에서 Redis 기반으로 전환

- 날짜: 2026-05-08
- 상태: 승인

## 컨텍스트

`StompSessionManager`는 `ConcurrentHashMap` 두 개로 `playerKey ↔ sessionId` 매핑을 관리한다.
멀티 서버 환경에서 이 구조는 다음 세 가지 문제를 일으킨다.

**1. GameRecovery API의 cross-instance 오판**

`POST /api/rooms/{joinCode}/recovery`는 HTTP 요청이므로 로드밸런서가 임의의 인스턴스로 라우팅한다.
세션 등록은 `SessionRegisteredEvent`를 Redis Stream에 발행하고 각 인스턴스가 비동기로 소비해 로컬 맵을 갱신하는 방식이다.
따라서 클라이언트가 연결 직후 recovery 요청을 보내면, 인스턴스 B가 아직 이벤트를 소비하지 못한 짧은 윈도우가 존재한다.
이 타이밍에 인스턴스 B가 요청을 받으면 `hasSessionId`가 `false`를 반환해 409를 응답한다.
인스턴스 B가 기동 후 consumer group offset 설정에 따라 과거 이벤트를 수신하지 못하는 경우에도 동일한 문제가 발생한다.
현재 구조는 **이벤트 기반 eventual consistency**이며 "지금 이 순간 연결 여부"를 묻는 질문에는 틀릴 수 있다.

**2. 방별 연결 플레이어 수 오집계**

`getConnectedPlayerCountByJoinCode`는 자신의 인메모리 맵만 순회하므로 전체 인스턴스의 연결 수를 합산할 수 없다.

**3. 신규 인스턴스 스타트업 시 상태 공백**

새 인스턴스가 기동될 때 기존 인스턴스에 등록된 세션 정보를 알지 못한다.
Redis Stream을 통한 이벤트 브로드캐스트(`SessionRegisteredEvent`)가 있지만 과거 이벤트는 수신하지 못한다.

**멀티 서버 메시지 전달 문제는 이미 해결되어 있음**

게임 이벤트는 모두 Redis Stream → 전 인스턴스 소비 → 로컬 SimpleBroker 전달 구조로 처리된다.
모든 Notifier/Publisher는 `/topic/room/{joinCode}/*` 브로드캐스트만 사용하며,
`convertAndSendToUser`(에러 응답)는 메시지를 수신한 인스턴스에서 즉시 응답하므로 cross-instance 라우팅이 필요 없다.
따라서 외부 STOMP 브로커(RabbitMQ 등)는 불필요하다.

## 결정

`StompSessionManager`의 저장소를 인메모리 `ConcurrentHashMap`에서 Redis로 교체한다.

**Redis 키 설계**

```text
ws:ps:{playerKey}   →  sessionId       (String, TTL = room.removalDelay)
ws:sp:{sessionId}   →  playerKey       (String, TTL = room.removalDelay)
ws:dc:{sessionId}   →  "1"             (String, TTL 60s — 중복 disconnect 방지)
```

개별 키(`ws:ps:*`, `ws:sp:*`)로 관리해 TTL을 세션 단위로 제어한다.

**SessionEventConfig 역할 변경**

기존에는 Redis Stream 이벤트를 수신해 로컬 맵을 동기화했다.
Redis가 단일 상태 저장소가 되므로 이 동기화 역할이 사라진다.
`sessionRegisteredEventConsumer`는 재연결 감지(기존 키 존재 여부 확인)와 `PlayerReconnectedEvent` 발행만 담당한다.

**`processedDisconnections` 처리**

기존 `ConcurrentHashMap.newKeySet()`을 `SET NX EX 60`으로 교체한다.
TTL 만료로 자동 정리되므로 별도 `remove` 호출이 필요 없다.

## 고려한 대안

| 대안                                         | 장점             | 단점                             |
|--------------------------------------------|----------------|--------------------------------|
| 현행 유지 (인메모리)                               | 변경 없음, 조회 O(1) | cross-instance 불일치, 스타트업 상태 공백 |
| Redis Hash 단일 키 (`HSET ws:player-session`) | 키 수 적음         | 세션 단위 TTL 불가 , HGETALL 비용      |
| 외부 STOMP 브로커 (RabbitMQ)                    | 메시지 라우팅까지 해결   | 추가 인프라, 메시지 전달은 이미 해결됨         |
| Redis Pub/Sub 라우팅 레이어 (C-1)                | 추가 인프라 없음      | 브로커 기능 직접 재구현, 높은 복잡도          |

## 트레이드오프

**장점**

- `hasSessionId`, `getConnectedPlayerCountByJoinCode`가 전 인스턴스 기준으로 정확해진다.
- 신규 인스턴스 스타트업 시 Redis에서 즉시 현재 상태를 읽는다.
- TTL 자동 만료로 인스턴스 크래시 후 고아 세션이 자동 정리된다.

**단점**

- 세션 조회마다 Redis I/O가 발생한다 (기존 인메모리 O(1) → 네트워크 레이턴시).
- Redis 장애 시 세션 등록/조회 전체가 영향을 받는다 (이미 ADR-0005에서 Redis 의존을 허용 범위로 판단).
- `DelayedPlayerRemovalService`의 `scheduledTasks`(`ConcurrentHashMap<String, ScheduledFuture<?>>`)는 여전히 인메모리다. 인스턴스 크래시 시 지연 삭제 태스크가 소실되는 문제는 이번 범위에서 제외하며 별도 ADR에서 다룬다.

## 결과

- `StompSessionManager`: `ConcurrentHashMap` 제거, `StringRedisTemplate` 주입. `registerPlayerSession`, `removeSession`, `hasPlayerKey`, `getPlayerKey`, `hasSessionId`, `getConnectedPlayerCountByJoinCode`, `isDisconnectionProcessed` 전 메서드가 Redis 연산으로 교체된다.
- `SessionEventConfig`: 로컬 맵 동기화 로직 제거. 재연결 감지는 `sessionManager.hasPlayerKeyInternal` 대신 Redis 조회로 처리.
- `application.yml`: 별도 설정 추가 없음. TTL은 기존 `room.removalDelay`를 주입받아 사용.
- 관련 단위 테스트(`StompSessionManagerTest`)는 `EmbeddedRedis` 또는 Testcontainers 기반으로 재작성한다.
