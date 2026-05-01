# ADR 인덱스

주요 기술 의사결정 목록. 새 ADR 작성은 `/adr [주제]`로 한다.

코드 작성 시 **영향 범위**와 작업 중인 패키지/주제를 비교한다. 겹치는 항목이 있으면 해당 ADR 파일을 읽어 핵심 제약과 충돌 여부를 확인한다.

| 번호                                                                | 제목                        | 상태 | 영향 범위                                                      | 핵심 제약                                                                      |
|-------------------------------------------------------------------|---------------------------|----|------------------------------------------------------------|----------------------------------------------------------------------------|
| [0001](adr/0001-ai-nickname-audit-with-operator-feedback-loop.md) | AI 닉네임 검열 + 운영자 피드백 루프 설계 | 승인 (후속 작업 예정) | `room/infra/nickname`, `room/application/service/nickname` | FLAGGED(confidence ≥ 0.85)는 자동 차단 후 어드민 해제 가능, PENDING(< 0.85)은 운영자 수동 결정 |
| [0002](adr/0002-block-stacking-game-design.md) | BlockStacking 게임 서버 설계 | 승인 | `blockstacking/`, `global/flow/`, `minigame/domain/MiniGameType` | 잘못된 progress 이벤트는 무시+warn 로그만, 20초 서버 타이머가 단일 종료 기준 |
| [0003](adr/0003-ladder-game-design.md) | LadderGame 게임 서버 설계 | 승인 | `laddergame/`, `minigame/domain/MiniGameType` | row는 서버 수신 순서로 결정, 인접 구간 동일 row 충돌 시 아래로 밀어 해소, DRAWING은 항상 5초 |
| [0004](adr/0004-roulette-adjustment-weight-http-configuration.md) | 룰렛 확률 조정 가중치(ADJUSTMENT_WEIGHT) HTTP 설정 가능화 | 승인 | `room/domain/roulette/`, `room/domain/`, `room/config/`, `room/ui/` | 유효 범위 0.1~0.9, READY 상태에서만 변경 가능, Room 어그리게이트가 adjustmentWeight 보유 |
