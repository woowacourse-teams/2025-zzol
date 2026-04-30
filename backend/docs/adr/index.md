# ADR 인덱스

주요 기술 의사결정 목록. 새 ADR 작성은 `/adr [주제]`로 한다.

코드 작성 시 **영향 범위**와 작업 중인 패키지/주제를 비교한다. 겹치는 항목이 있으면 해당 ADR 파일을 읽어 핵심 제약과 충돌 여부를 확인한다.

| 번호                                                                | 제목                        | 상태 | 영향 범위                                                      | 핵심 제약                                                                      |
|-------------------------------------------------------------------|---------------------------|----|------------------------------------------------------------|----------------------------------------------------------------------------|
| [0001](0001-ai-nickname-audit-with-operator-feedback-loop.md) | AI 닉네임 검열 + 운영자 피드백 루프 설계 | 승인 (후속 작업 예정) | `room/infra/nickname`, `room/application/service/nickname` | FLAGGED(confidence ≥ 0.85)는 자동 차단 후 어드민 해제 가능, PENDING(< 0.85)은 운영자 수동 결정 |
| [0002](0002-block-stacking-game-design.md)                    | BlockStacking 게임 서버 설계 | 승인 | `blockstacking/`, `global/flow/`, `minigame/domain/MiniGameType` | 잘못된 progress 이벤트는 무시+warn 로그만, 20초 서버 타이머가 단일 종료 기준 |
| [0003](0003-ladder-game-design.md)                            | LadderGame 게임 서버 설계 | 승인 | `laddergame/`, `minigame/domain/MiniGameType` | row는 서버 수신 순서로 결정, 인접 구간 동일 row 충돌 시 아래로 밀어 해소, DRAWING은 항상 5초 |
| [0004](0004-oauth2-login-and-user-domain.md)                  | OAuth2 로그인 도입과 User 도메인 분리 | 승인 | `user/`, `room/infra/persistence/`, `auth/` | Player-User는 nullable FK로 분리, 회원 입장 시 프로필 닉네임 강제, 익명 흐름 그대로 유지 |
| [0005](0005-jwt-refresh-token-policy.md)                      | Access(JWT) + Refresh(Redis) 인증 토큰 정책 | 승인 | `user/config/`, `user/application/service/`, `user/infra/redis/` | Refresh token은 `{userId}:{tokenId}` 형식, 재사용 감지 시 family 전체 폐기 |
| [0006](0006-user-code-policy.md)                              | 5자리 사용자 식별 코드(UserCode) 정책 | 승인 | `user/domain/`, `user/domain/service/` | UserCode는 가입 시 1회 발급 후 불변, INSERT-then-catch + 최대 100회 재시도 |
