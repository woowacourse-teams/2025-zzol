# 0006. 5자리 사용자 식별 코드(UserCode) 정책

- 날짜: 2026-04-30
- 상태: 승인

## 컨텍스트

회원에게 영구적이고 사람이 읽을 수 있는 식별자가 필요했다.
DB auto-increment PK는 순차 노출 문제가 있고, UUID는 사람이 읽기 어렵다.
기존 `JoinCode`(5자리, 영문+숫자 조합)가 이미 잘 동작하고 있어 동일 패턴을 차용하기로 했다.

## 결정

- **형식**: 영문 대문자 + 숫자 조합 5자리 (`JoinCode.CHARSET` 동일).
- **불변**: 발급 이후 수정 불가. `User` 도메인에 setter 없음.
- **고유성 보장**: DB `UNIQUE` 제약 + `INSERT-then-catch` 방식으로 race condition 방지.
- **재시도**: 중복 시 최대 100회 재시도 (`UserCodeProperties.maxRetry`로 설정 가능).
- **메트릭**: `userCode.generation.success/duplication/max_retry_exceeded` 카운터로 모니터링.

## 고려한 대안

| 대안                   | 장점     | 단점                        |
|----------------------|--------|---------------------------|
| UUID (36자)           | 충돌 없음  | 사람이 읽기 어려움. 복사·공유 불편      |
| auto-increment PK 노출 | 구현 없음  | 순차 노출로 회원 수 추정 가능. 보안 취약  |
| SELECT-then-INSERT   | 로직 직관적 | race condition 시 중복 삽입 가능 |

## 트레이드오프

**장점**

- `JoinCodeGenerator` 구조를 그대로 재사용해 검증된 패턴 적용.
- 5자리로 사람이 구분하기 쉬우며 공유·입력이 용이.
- INSERT 실패 catch 방식으로 race condition을 DB 레벨에서 안전하게 처리.

**단점**

- 이론상 중복 가능성 존재(영문 대문자+숫자 5자리 = 약 6천만 조합). 회원 수가 수백만에 달하면 재시도 빈도 증가.
- 100회 초과 시 `BusinessException`으로 가입 실패. 실제 도달 가능성은 낮으나 운영 모니터링 필요.

## 결과

- `UserCode` record VO가 신설되었으며 `User` 생성 시 주입 필수.
- `UserCodeGenerator`가 `UserRegistrationService`에 주입되어 신규 가입 시 자동 발급.
- `app_user.user_code CHAR(5) UNIQUE NOT NULL` 제약이 DB에 적용되었다.
