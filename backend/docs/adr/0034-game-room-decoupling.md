# 0034. `:game → :room` 잔여 의존 제거 — 존재검증·상태전이·JPA FK 분리

- 날짜: 2026-07-10
- 상태: 승인 (구현 완료 — #1548, 머지 대기)
- 참조: ADR-0025(방-게임 분리, 본 결정이 유예한 JPA FK 계열을 이어받음), 이슈 #1547(`:game → :user` 이벤트 역전 — 자매 작업), 이슈 #1548(본 작업)

## 컨텍스트

ADR-0025는 방과 게임 세션의 소유권을 분리하며 `:game → :room` 의존 대부분을 in-process 동기 이벤트(`:game-api`)로 역전했다. 그러나 개정(2026-06-10)에서 **JPA 외래키(FK) 계열 의존은 명시적으로 범위 밖으로 유예**했다.

> ADR-0025 본문: "`MiniGamePersistenceService`의 `Room`/`RoomState`/`PlayerEntity` 의존(JPA FK 계열)은 본 개정 범위 밖이며, `MiniGameEntity`의 `RoomEntity` FK·`PlayerEntity` 영속 책임 분리는 별도 후속 작업으로 미룬다."

그 결과 현재 `:game` 프로덕션 코드는 6개 파일에서 `:room`을 참조하며, 성격이 다른 3개 층위로 나뉜다.

| 층위 | 위치 | `:room` 참조 | 성격 |
|------|------|------------|------|
| L3 존재검증 | `MiniGameRestController` | `RoomQueryService.validateRoomExists` | 방 존재 확인(조회) |
| L3 상태전이 | `MiniGamePersistenceService` | `RoomStatusPort.updateStatus(RoomEntity, RoomState.PLAYING)`, `RoomEntityRepository`, `RoomState` | 게임 시작 시 방을 PLAYING으로 전이 |
| L2 FK 조회 | `MiniGameResultSaveEventListener`, `MiniGameEntityRepository` | `RoomJpaRepository`, `PlayerJpaRepository`, `RoomEntity` 쿼리 파라미터 | FK 대상 엔티티 조회 |
| L1 JPA FK | `MiniGameEntity`, `MiniGameResultEntity` | `@ManyToOne RoomEntity roomSession`, `@ManyToOne PlayerEntity player` | DB 외래키 **객체 참조** |

`:game → :user`는 이벤트 역전으로 제거했고(#1547), 이제 `:room`이 `:game`의 마지막 도메인-모듈 의존이다. 목표는 **`:game`이 도메인 모듈(`:room`·`:user`)에 의존하지 않고 공유 플랫폼(`:game-api`·`:infra`·`:web`·`:websocket`·`:common`)에만 의존**하는 것이다. (문자 그대로 "`:game-api`에만"은 불가 — 게임 모듈은 여전히 JPA·HTTP·STOMP 플랫폼이 필요하다.)

## 결정

세 층위를 각각 아래 방식으로 처리한다.

### 1. 존재검증 — `RoomQueryService` → `GameSession` 조회

`MiniGameRestController`가 방 존재를 `RoomQueryService.validateRoomExists`로 확인하던 것을, `:game`이 이미 소유한 `GameSession`(`GameSessionService`) 조회로 대체한다. 게임 API의 관심사는 "이 방에 진행 중/생성된 게임 세션이 있는가"이므로 `:game` 자체 상태로 판단할 수 있다. → `RoomQueryService` 참조 제거.

### 2. 상태전이·영속 — 게임 시작 이벤트 전파, `:room`이 수신 처리

`MiniGamePersistenceService`가 `RoomEntity`를 조회해 `roomStatusPort.updateStatus(roomEntity, RoomState.PLAYING)`로 방을 직접 전이하던 것을 제거한다. `:game`은 게임 시작 이벤트(`:game-api`)만 발행하고, **`:room`이 이를 수신해 자기 `RoomEntity` 상태전이·영속을 수행**한다. 이는 ADR-0025가 `GameSessionStartedEvent → RoomGameStartListener(markPlaying)`로 확립한 패턴을 이 경로까지 확장·완성하는 것이다. → `RoomStatusPort`·`RoomEntityRepository`·`RoomState`·`RoomEntity` 참조 제거.

### 3. JPA FK — 객체 참조 → `Long` ID 참조

`MiniGameEntity.roomSession(@ManyToOne RoomEntity)` → `Long roomSessionId`, `MiniGameResultEntity.player(@ManyToOne PlayerEntity)` → `Long playerId`로 바꾼다. **DB 외래키 컬럼·제약은 그대로 유지**하고 JPA 연관 탐색만 끊는다. 리포지토리 시그니처의 `RoomEntity`/`PlayerEntity` 파라미터는 `Long`으로 바꾼다(`findByRoomSessionIdAndMiniGameType(Long, …)`).

**ID 공급(핵심 설계점).** `:game`이 `:room` 리포지토리를 직접 조회하지 않고 id를 얻는 방법 —
`roomSessionId`·`playerId` **둘 다 `:game-api`의 단일 조회 포트 `RoomSnapshotQuery`로 통일**하고 `:room`이 구현한다(이벤트 페이로드 확장은 불필요).

```java
// :game-api
interface RoomSnapshotQuery {
    long resolveRoomSessionId(String joinCode);
    List<PlayerSnapshot> resolvePlayers(long roomSessionId, List<String> playerNames);
    record PlayerSnapshot(String playerName, long playerId) {}
}
```

- `MiniGamePersistenceService`(게임 시작 저장)는 `resolveRoomSessionId`로 `MiniGameEntity`의 `roomSessionId`를 얻는다.
- `MiniGameResultSaveEventListener`(결과 저장)는 `resolveRoomSessionId`+`resolvePlayers`로 `MiniGameResultEntity`의 `playerId`를 얻어 `RoomJpaRepository`·`PlayerJpaRepository` 직접 조회를 제거한다. 통계 이벤트용 `userId`는 `:room`이 아니라 `Gamer`(game-api)가 이미 들고 있으므로 `Gamer.getUserId()`에서 얻는다 — 포트는 영속 식별자(roomSessionId·playerId)만 공급한다.
- 구현체 `RoomSnapshotQueryAdapter`(`:room`)는 `RoomEntityRepository`와 신규 파생 쿼리 `findByRoomSession_IdAndPlayerNameIn`로 id를 조회한다.

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| 현행 유지 (FK 예외 존치) | 변경 0, JPA 객체 탐색 편의 | `game→room` 상시 결합, 재빌드·재테스트 전파, ADR-0025 유예 미해소 |
| L3만 역전 (FK 유지) | 중간 비용, 결합 표면 축소 | 최종 목표(도메인 의존 0) 미달, `room.infra` 참조 잔존 |
| **ID 참조 전환 (채택)** | 컴파일 의존 제거, 경계 명확, 프로덕션 재테스트 결합 완화 | JPA 탐색 상실, id 공급 메커니즘 추가, 참조 무결성이 앱→DB 제약으로 이동, 매핑·쿼리 회귀 리스크 |
| 게임 영속을 `:room`으로 이관 | FK 자연 유지 | 게임 결과 영속을 `:room`이 소유 = 소유권 역전, ADR-0025 취지 위배 |

## 트레이드오프

- **JPA 연관 탐색 상실**: `miniGameEntity.getRoomSession()` 같은 네비게이션이 불가해지고, 필요 시 명시적 조회로 대체한다. 대신 경계가 명확해지고 LAZY 로딩·N+1 리스크가 준다.
- **참조 무결성 위치 이동**: 애플리케이션 레벨 연관이 사라지고 DB FK 제약만 남는다. 잘못된 id 저장은 컴파일이 아닌 런타임 제약 위반으로 드러난다.
- **id 공급 복잡도 증가**: 조회 포트(`RoomSnapshotQuery`)가 늘어난다. 다만 이는 ADR-0025가 이미 채택한 포트 역전 패턴의 연장이며 새로운 개념이 아니다.
- **분석 쿼리 파급**: `:admin` 대시보드 통계(QueryDSL)가 `MINI_GAME.roomSession`·`MINI_GAME_RESULT.player` 연관 탐색으로 조인하던 것을 `Long` id 명시 조인(`.on(ROOM.id.eq(MINI_GAME.roomSessionId))` 등)으로 바꿔야 한다 — JPA 탐색 상실의 대표적 파급점.
- **마이그레이션 리스크**: 스키마 컬럼은 불변이라 데이터 영향은 없으나, 매핑·쿼리 변경으로 회귀 위험이 있어 결과 저장·조회 경로 통합 테스트가 필수다.

## 결과

- `:game` 프로덕션의 `:room` 참조 6파일이 모두 제거되어 `game/build.gradle.kts`의 `implementation(project(":room"))`를 제거할 수 있다. `:game`은 도메인 모듈 의존이 0이 된다.
- **ArchUnit 규칙 강화**: `GameArchitectureTest`의 `minigame.application`용 **`room.infra` FK 예외 규칙을 제거**하고, 예외 없는 브로드 규칙 `game_프로덕션은_room을_직접_참조할_수_없다`(game 패키지 전체 → `coffeeshout.room..` 금지)로 대체했다. `game_프로덕션은_user를_직접_참조할_수_없다`(#1547)와 짝을 이뤄 재유입을 차단한다.
- **프로덕션 재테스트 결합 완화**: `:room` 변경 시 `:game:test`가 무효화되던 결합(#1547 논의)이 프로덕션 경로에서는 사라진다. 단, `@SpringBootTest` 전체 스캔 컨텍스트가 여전히 전이 `:room` 빈을 로드하면 `testImplementation(project(":room"))`는 테스트 스코프로 남을 수 있다(프로덕션 방향과 무관, #1547의 `:user`와 동일한 성격).
- `:room → :user` 정리와 독립적으로 진행 가능하다.
