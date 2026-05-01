# 0004. 룰렛 확률 조정 가중치(ADJUSTMENT_WEIGHT) HTTP 설정 가능화

- 날짜: 2026-05-01
- 상태: 승인

## 컨텍스트

`ProbabilityCalculator`는 미니게임 등수에 따라 룰렛 당첨 확률을 조정할 때 `ADJUSTMENT_WEIGHT = 0.7`을 정적 상수로 사용한다. 이 값은 조정 강도를 결정하는 핵심 파라미터로, 0에 가까울수록 모든 플레이어가 균등한 확률을 갖고, 1에 가까울수록 1등과 꼴등의 확률 차이가 극대화된다.

이 값이 하드코딩되어 있어 두 가지 문제가 있다.

- 프로덕션 컨벤션 위반: "조정 가능한 값은 `application.yml` + `@ConfigurationProperties`. 하드코딩 금지"
- 게임 공정성 조율 불가: 호스트가 게임 성격(가볍게 즐기기 vs 실력 반영 강화)에 맞게 가중치를 조정할 수 없다

가중치를 HTTP 요청으로 변경 가능하게 만드는 설계가 필요하다.

## 결정

가중치는 방(Room) 어그리게이트의 설정으로 관리한다. 게임 공정성은 방 단위 관심사이므로 이 설계가 의미론적으로 자연스럽다.

- 기본값 0.7을 `application.yml`의 `roulette.default-adjustment-weight`로 이동하고 `RouletteProperties`(`@ConfigurationProperties`)가 주입받는다
- `Room` 도메인에 `adjustmentWeight` 필드를 추가한다. 기본값은 방 생성 시 외부에서 주입받는다 (도메인이 `RouletteProperties`를 직접 import하지 않도록 `double` 원시 값으로 전달)
- 호스트는 READY 상태에서 HTTP 요청으로 가중치를 변경할 수 있다
- `ProbabilityCalculator` 생성자는 정적 상수 대신 파라미터로 `adjustmentWeight`를 받는다
- 유효 범위: 0.1 이상 0.9 이하

흐름 요약:

```text
RouletteProperties (application.yml 기본값)
  └─ Room 생성 시 adjustmentWeight 주입
       └─ HTTP PATCH 으로 호스트가 READY 상태에서 변경 가능
            └─ Room.applyMiniGameResult()
                 └─ new ProbabilityCalculator(playerCount, roundCount, adjustmentWeight)
```

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| READY 상태에서 수시 변경 가능 (채택) | 게임 시작 전까지 세밀한 조정 가능 | 호스트가 여러 번 바꾸면 최종값만 유효하다는 것이 명확해야 함 |
| 방 생성 시 1회 고정 | 단순, 생성 후 불변 | 게임 시작 전 재조정 불가 |

## 트레이드오프

**감수한 것들**

- 가중치의 수학적 의미(0.7 = 최대 조정폭의 70%)가 호스트에게 직관적이지 않다. UI에서 "공정성 수준"처럼 사용자 친화적 레이블로 포장할 책임은 클라이언트에게 있다.
- 첫 번째 미니게임이 시작된 이후(PLAYING 상태)에는 변경이 불가하다. READY 상태 제약이 이를 보장한다.

**얻은 것들**

- 하드코딩 제거로 컨벤션 준수.
- 호스트가 게임 특성에 맞게 조정 강도를 런타임에 제어할 수 있다.
- 인메모리 `Room` 어그리게이트에 필드만 추가하므로 DB 스키마 변경이 없다.

## 결과

- `room/config/RouletteProperties.java` 추가 (`roulette.default-adjustment-weight`, 기본 0.7)
- `Room.adjustmentWeight` 필드 추가, 생성 시 기본값 주입
- `Room.updateAdjustmentWeight(double)` 메서드 추가 (READY 상태 검증 포함)
- `ProbabilityCalculator(int playerCount, int roundCount, double adjustmentWeight)` 생성자로 변경
- HTTP 엔드포인트: `PATCH /api/rooms/{joinCode}/settings` — `adjustmentWeight` 필드, 호스트 전용, READY 상태에서만 허용
- 영향 범위: `room/domain/roulette/`, `room/domain/`, `room/config/`, `room/ui/`
