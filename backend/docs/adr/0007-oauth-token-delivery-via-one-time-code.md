# 0007. OAuth 로그인 토큰 전달 — 일회용 코드 교환 방식

- 날짜: 2026-05-03
- 상태: 승인

## 컨텍스트

OAuth2 로그인 성공 후 서버는 Access Token과 Refresh Token을 클라이언트에 전달해야 한다.
초기 구현에서는 `OAuthSuccessHandler`가 토큰을 쿼리 파라미터에 담아 프론트엔드로 리다이렉트했다.

```text
https://zzol.site/oauth/callback?accessToken=eyJ...&refreshToken=abc...
```

이 방식은 다음 보안 위협에 노출된다.

- 브라우저 히스토리에 토큰이 평문으로 기록됨
- 서버 액세스 로그, CDN 로그에 토큰이 노출됨
- `Referer` 헤더를 통해 외부 도메인으로 토큰이 유출될 수 있음

## 결정

OAuth 성공 시 토큰을 직접 전달하지 않고 **일회용 코드(one-time code)** 를 경유하는 2단계 방식으로 변경한다.

1. `OAuthSuccessHandler`가 UUID 코드를 생성하고, `{code → TokenPair}`를 Redis에 TTL 30초로 저장한다.
2. 프론트엔드로 코드만 담아 리다이렉트한다.

```text
https://zzol.site/oauth/callback?code=550e8400-e29b-41d4-a716-446655440000
```

3. 프론트엔드가 `POST /auth/token { "code": "..." }` 를 호출하면 서버는 Redis에서 코드를 **getAndDelete**(원자적 조회 후 삭제)하고 응답한다.
   - Access Token: 응답 바디
   - Refresh Token: `HttpOnly; Secure; SameSite=None` 쿠키

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| 쿼리 파라미터에 토큰 직접 전달 | 구현 단순 | 브라우저 히스토리·로그·Referer에 토큰 노출 |
| URL Fragment(`#`)에 토큰 전달 | 서버 로그 노출 없음 | 브라우저 히스토리에 기록됨. JavaScript 접근 가능하여 XSS에 취약 |
| Refresh Token도 바디로 전달 | 쿠키 설정 불필요 | 프론트엔드가 직접 저장해야 하므로 XSS 탈취 위험 |
| 일회용 코드 교환 (채택) | URL에 토큰 미노출. Refresh Token HttpOnly 쿠키 전달 가능 | 추가 API 호출 1회. Redis 의존성 추가 |

## 트레이드오프

**장점**

- 토큰이 URL에 노출되지 않아 브라우저 히스토리·서버 로그 노출 위험 제거.
- Refresh Token을 `HttpOnly` 쿠키로 전달해 JavaScript에서 접근 불가.
- 코드는 TTL 30초 + 1회 사용 후 즉시 삭제(getAndDelete)되어 재사용 불가.

**단점**

- OAuth 콜백 후 `/auth/token` 추가 API 호출이 발생해 로그인 완료까지 네트워크 왕복이 1회 늘어남.
- Redis 장애 시 로그인 불가 (ADR 0004의 Refresh Token 정책과 동일한 제약).
- 30초 TTL 이내에 코드 교환을 완료하지 못하면 로그인 실패. 다시 OAuth 플로우를 시작해야 함.

## 결과

- `OAuthCodeRepository`: `save(code, TokenPair, ttlSeconds)` / `findAndDelete(code)` 인터페이스.
- `RedisOAuthCodeRepository`: `oauth:code:{code}` 키로 저장, getAndDelete 원자 연산 구현.
- `AuthTokenService.issueCode(User)`: 토큰 발급 후 30초 코드로 래핑하여 반환.
- `AuthTokenService.exchangeCode(String)`: 코드 교환. 코드 없으면 `OAUTH_CODE_NOT_FOUND(401)`.
- `POST /auth/token`: 코드 교환 엔드포인트. Access Token 바디 + Refresh Token HttpOnly 쿠키 응답.
- `POST /auth/refresh`: Refresh Token을 쿠키에서 읽도록 변경 (기존 바디 방식에서 변경).
