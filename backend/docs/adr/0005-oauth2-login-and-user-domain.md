# 0005. OAuth2 로그인 도입과 User 도메인 분리

- 날짜: 2026-04-30
- 상태: 승인

## 컨텍스트

기존 시스템은 모든 사용자가 익명이었고, 입장 코드 + PlayerName 조합으로만 식별되었다.
영속적 회원 식별, 프로필 관리, 건의사항 작성자 추적 등 회원 개념이 필요해짐에 따라
OAuth2 소셜 로그인과 User 도메인을 도입하게 되었다.

핵심 제약 조건으로, 기존 익명 게임 참여 흐름(JoinCode + PlayerName)은 그대로 유지해야 했다.

## 결정

- **Google, Kakao, Naver** OAuth2 소셜 로그인을 `spring-boot-starter-oauth2-client`로 구현한다.
- `User`와 `Player`를 분리하고, `PlayerEntity.user_id`를 nullable FK로 설계한다 (익명 호환).
- 회원이 방에 입장할 때는 프로필 닉네임이 PlayerName으로 강제 적용되며, 입장 시점에 스냅샷된다.
- 카카오는 OIDC 미준수로 `CustomOAuth2UserService`에서 provider별 `OAuth2UserConverter`로 분기한다.

## 고려한 대안

| 대안                      | 장점       | 단점                                            |
|-------------------------|----------|-----------------------------------------------|
| Player에 회원 정보 직접 통합     | 구조 단순    | 익명/회원 분기 로직이 Player 전반에 퍼짐. 익명 게임 흐름 수정 범위 과다 |
| 자체 이메일/비밀번호 인증          | 외부 의존 없음 | 이메일 인증, 비밀번호 찾기 등 부가 기능 구현 비용이 매우 큼           |
| User-Player 완전 통합 (1:1) | 조회 단순    | 익명 사용자는 User 없이 Player만 존재하는 구조와 충돌           |

## 트레이드오프

**장점**

- 기존 익명 흐름을 전혀 수정하지 않아도 되어 회귀 위험이 낮다.
- OAuth2 표준 흐름을 활용하므로 provider 추가가 쉽다.

**단점**

- `PlayerEntity.user_id` nullable로 인해 회원/익명 분기가 서비스 레이어 곳곳에 남는다.
- 카카오 OIDC 미준수로 provider별 변환 코드를 별도 유지해야 한다.
- 회원 닉네임 변경이 진행 중인 방의 PlayerName에는 즉시 반영되지 않는다 (입장 시점 스냅샷 정책).

## 결과

- `coffeeshout.user` 패키지가 신설되었다 (domain, application, infra, ui, config).
- `PlayerEntity`에 `user_id BIGINT NULL` 컬럼이 추가되었다.
- 회원으로 방에 입장하면 `user_id`가 채워지고, in-room 닉네임 변경이 거부된다.
- `SecurityConfig`에 사용자 체인이 `@Order(2)`로 추가되었으며, `anyRequest().permitAll()`로 익명 호환이 유지된다.
