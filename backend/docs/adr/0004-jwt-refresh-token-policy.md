# 0004. Access(JWT) + Refresh(Redis) 인증 토큰 정책

- 날짜: 2026-04-30
- 상태: 승인

## 컨텍스트

OAuth2 로그인 성공 후 API 인증을 어떻게 처리할지 결정해야 했다.
서버가 Stateless하게 동작하면서도 강제 로그아웃(전체 기기)과 탈취 감지가 가능해야 했다.

## 결정

- **Access Token**: HS256 서명 JWT, 만료 30분. 클레임에 `userId`, `userCode`만 포함한다 (닉네임 제외).
- **Refresh Token**: UUID 기반 불투명 토큰, Redis에 저장, TTL 14일.
- 클라이언트에 전달되는 refresh token 값은 `{userId}:{tokenId}` 형식이다.
- **Rotation 정책**: 매 갱신 시 기존 토큰 삭제 후 새 토큰 발급.
- **Family 폐기**: 이미 사용된(삭제된) 토큰이 재사용되면 해당 userId의 모든 토큰을 폐기한다.

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| Access Token만 사용 (세션 없음) | 구조 단순 | 만료 전 강제 로그아웃 불가. 탈취 시 만료까지 유효 |
| Refresh Token도 JWT로 발급 | Redis 불필요 | 서버에서 폐기 불가. 탈취 감지 어려움 |
| DB 기반 Refresh Token | Redis 불필요 | 갱신마다 DB 쓰기. 트래픽 높을 때 병목 |
| 닉네임을 Access Token 클레임에 포함 | `/users/me` 조회 절감 | 닉네임 변경 시 토큰 재발급 필요. 즉시 반영 불가 |

## 트레이드오프

**장점**

- Redis TTL로 자동 만료 처리, 별도 배치 불필요.
- Family 폐기로 탈취된 토큰 재사용 시 해당 유저 전체 세션을 즉시 무효화.
- `{userId}:{tokenId}` 형식으로 Redis에서 삭제된 후에도 userId 파싱이 가능해 family 폐기가 가능.

**단점**

- Redis 장애 시 로그인/토큰 갱신 불가 (가용성 의존).
- Access Token 30분 만료 전 강제 로그아웃은 불가능 (허용 범위로 판단).
- 닉네임 변경은 Access Token 만료(최대 30분) 후에야 새 토큰에 반영된다.

## 결과

- `AuthTokenService`: `issue`, `rotate`, `revoke`, `verify` 4개 오퍼레이션.
- `JwtAuthenticationFilter`: `Authorization: Bearer` 헤더 검증, 토큰 없으면 익명으로 통과.
- `POST /auth/refresh`: rotation 엔드포인트.
- `POST /auth/logout`: 해당 유저의 모든 refresh token 폐기.
