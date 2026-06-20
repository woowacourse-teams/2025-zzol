# 0001. 게임 통합테스트 플레이키 — poison-message 폭풍과 스케줄러 기아 오진

- 날짜: 2026-06-11
- 심각도: P2 (CI 생산성 저하, 사용자 영향 없음)
- 상태: 완료
- 관련 ADR: [0013 도메인 모듈 테스트 독립 실행 전략](../adr/0013-domain-module-test-isolation.md)
- 관련 포스트모템: [0002 Testcontainers reuse 의사결정 2회 번복](0002-testcontainers-reuse-decision-reversal.md)

## 요약

`be/refactor/game-room-to-joincode`(PR #1397, Room-GameSession 분리) 머지 전후로 `:game` 모듈 WebSocket 통합테스트 4종(BlindTimer / BlockStacking / CardGame / Ladder)이 CI에서 awaitility `ConditionTimeoutException`으로 간헐 실패했다. 로컬 `:game` 단독 실행은 항상 통과했고 실패 메서드가 런마다 달라지는 전형적 플레이키였다. 진단은 세 번 빗나간 끝에, 근본 원인이 **공유 컨테이너 reuse + 멀티모듈 동시실행**으로 인한 stale 이벤트의 스트림 컨슈머 풀 포화(=게임 이벤트 처리 기아)임을 로컬 재현으로 확정했다.

## 타임라인

| 시점 | 사건 |
|------|------|
| #1364 | CI에서 Testcontainers `reuse` 활성화(모듈별 컨테이너 중복 기동 제거). 이때부터 컨테이너가 런 간 공유됨 |
| #1397 머지 전후 | `:game` WebSocket 통합테스트 4종이 CI에서 간헐 타임아웃. 로컬 단독은 통과 |
| 진단 1차 | `io.lettuce.core.RedisException: Connection closed` 48건을 원인으로 의심 |
| 진단 2차 | Redis 소비/폴링 풀을 8/16 → 16/32로 증설(커밋 7f219e6a). 실패 지속 |
| 진단 3차 | 페이즈 전이 스케줄러 풀(기본 2) 기아로 의심 |
| 2026-06-11 | `./gradlew test --rerun-tasks`(전 모듈 병렬)로 RacingGameIntegrationTest 재현 성공. 재현 매트릭스로 트리거를 reuse로 확정 |
| 커밋 c2bed5b2 | `assignQrCode`/`assignQrCodeError`가 삭제된 방의 QR 이벤트를 멱등 skip하도록 수정 — 로컬 QR poison 폭풍 제거 검증(실패 15+ → 0) |
| PR #1411 | 게임 통합테스트가 subscribe 등록 완료를 per-topic으로 대기하도록 수정(타이밍 결함 해소) |

## 임팩트

- CI에서 `:game` 통합테스트가 비결정적으로 적색이 되어 PR 머지가 재실행에 의존. 개발 속도 저하.
- 사용자 대면 영향 없음. 프로덕션 코드 결함이 아니라 테스트 격리 결함이었다(단, QR 멱등 처리 누락은 프로덕션 견고성 측면에서도 c2bed5b2로 함께 보강).

## 근본 원인

`reuse` 활성으로 Valkey 컨테이너가 런 간 공유되는 상태에서 멀티모듈 테스트가 동시 실행되면, 이전 런 또는 다른 모듈이 남긴 **stale/외부 이벤트가 공유 Redis Stream에 잔존**한다. 대표 사례는 비동기 QR 생성이 테스트 경계를 넘어 도착하는 `QrCodeStatusEvent`다.

`@BeforeEach`의 `flushDb()`로 방은 사라졌지만 스트림에 남은 이벤트를 컨슈머가 처리하려다 "방이 존재하지 않음"으로 실패하고, `EventDispatcher`가 예외를 swallow + ack 하는 구조 탓에 동일 외부 이벤트가 반복 적재·재처리된다. 이 반복이 **스트림 컨슈머(`concurrent`) 풀을 점유**해, 정작 게임 진행 이벤트(블록 커맨드 등 WS → 스트림 → 컨슈머 경로)가 처리되지 못하고 굶는다. 그 결과 awaitility 조건이 타임아웃한다.

재현 매트릭스(로컬, 2026-06-11):

| 컨테이너 격리 | 결과 |
|--------------|------|
| Valkey + MySQL 공유 (reuse on) | 실패 |
| Valkey만 분리 | stale 오염 0이나 MySQL 경합으로 실패 |
| 둘 다 분리 (reuse off) | 2/2 통과 |

즉 Valkey 오염(stale 이벤트)과 MySQL 경합 **둘 다** 격리가 필요했다. 스케줄러 풀도, 소비풀 크기도, CPU도, 결과 저장 부하도 근본 원인이 아니었다.

## 대응

1. **오진 배제** — lettuce `Connection closed` 48건은 `SpringApplicationShutdownHook` 시점의 종료 노이즈로 확인해 배제. 페이즈 전이가 인메모리 `SimpleBroker` 직접 전송(`messagingTemplate.convertAndSend`)임을 확인해 "동기 Stream 발행" 가설도 폐기.
2. **프로덕션 견고성 보강**(c2bed5b2) — 삭제된 방의 QR 이벤트를 throw 없이 멱등 skip. base64 페이로드 로깅 제거. 단위테스트 3개 추가, `:room:test` 통과.
3. **타이밍 결함 수정**(#1411) — 통합테스트가 STOMP subscribe 등록 완료를 per-topic으로 대기하도록 보강.

## 재발 방지 액션

| 액션 | 상태 |
|------|------|
| 삭제된 방 대상 QR 이벤트 멱등 skip (프로덕션 경로) | ☑ c2bed5b2 |
| subscribe 등록 완료 대기로 테스트 타이밍 결함 제거 | ☑ #1411 |
| 컨테이너 격리(reuse) 정책 결정 — 의사결정 과정은 별도 회고로 | → [0002](0002-testcontainers-reuse-decision-reversal.md) |
| `EventDispatcher`의 예외 swallow + ack로 인한 중복 적재 정확한 발생점 규명 | ☐ #1361 영역, 미해결 |

## 교훈

- **"로컬 통과 / CI 실패 + 실패 대상 런마다 변동"은 코드 버그가 아니라 격리·타이밍 신호다.** 단일 테스트를 들여다보기 전에 실행 환경(공유 자원, 동시성)을 먼저 의심한다.
- **로컬 재현 없이 풀 크기부터 키우지 않는다.** 소비풀 증설(진단 2차)은 근본 원인과 무관해 시간만 소모했다. 재현 매트릭스로 변수를 하나씩 분리하는 편이 빨랐다.
- 로컬에서 잡은 poison 이벤트(`QrCodeStatusEvent`/QYLZ)는 CI의 것(`BlockStackingCommandEvent` 파싱 실패)과 **동일 인스턴스가 아니다.** 같은 클래스·같은 메커니즘(공유 스트림 stale 이벤트 → 컨슈머 포화 → 게임 기아)을 재현한 것이지 개별 이벤트는 다르다는 점을 회고에 명시해 둔다.
