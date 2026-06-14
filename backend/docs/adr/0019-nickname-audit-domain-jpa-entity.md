# 0019. NicknameAudit / NicknameFeedback — JPA 어노테이션을 가진 도메인 객체

- 날짜: 2026-05-26
- 상태: 승인

## 컨텍스트

`:profanity` 모듈에 ArchUnit 레이어 규칙이 추가됐다.

```text
profanity.application은 profanity.infra를 직접 참조할 수 없다
```

그런데 `NicknameAuditEntity`와 `NicknameFeedbackEntity`는 `infra.persistence.audit` 패키지에 있었고,
application 레이어의 여러 클래스(서비스, 포트 인터페이스)가 이 타입을 직접 사용했다.

```text
coffeeshout.profanity.application.ProfanityAuditService
coffeeshout.profanity.application.ProfanityFeedbackService
coffeeshout.profanity.application.ProfanityAuditBatchProcessor
coffeeshout.profanity.application.port.NicknameAuditRepository
coffeeshout.profanity.application.port.NicknameFeedbackRepository
```

포트 인터페이스가 infra 타입을 시그니처에 올리는 것은 헥사고날 아키텍처의 의도에 반한다.
두 엔티티는 단순한 상태 저장 객체로, 별도의 비즈니스 로직을 갖지 않는다.

## 결정

`NicknameAuditEntity`, `NicknameFeedbackEntity`를 각각 `NicknameAudit`, `NicknameFeedback`으로 이름을 바꾸고
`coffeeshout.profanity.domain.audit` 패키지로 이전한다.
JPA 어노테이션(`@Entity`, `@Table`, `@Column` 등)은 제거하지 않고 그대로 유지한다.

```text
변경 전: coffeeshout.profanity.infra.persistence.audit.NicknameAuditEntity
변경 후: coffeeshout.profanity.domain.audit.NicknameAudit

변경 전: coffeeshout.profanity.infra.persistence.audit.NicknameFeedbackEntity
변경 후: coffeeshout.profanity.domain.audit.NicknameFeedback
```

`NicknameAuditJpaRepository`, `NicknameFeedbackJpaRepository`는 `infra.persistence.audit` 패키지에 남는다.
JPA 레포지토리는 Spring Data 인프라에 해당하므로 도메인 객체를 받는 것이지 그 자체가 도메인은 아니다.

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| 도메인 객체와 JPA 엔티티 분리 (Mapper 도입) | domain 레이어가 JPA에 독립, 테스트 시 순수 POJO 사용 가능 | 매핑 코드 추가 (~200줄 이상), 필드 변경 시 두 클래스를 동시에 수정해야 함 |
| **JPA 어노테이션을 가진 도메인 객체 (채택)** | 매핑 코드 없음, 변경 지점 최소화, ArchUnit 통과 | domain 레이어가 `jakarta.persistence`에 의존 |
| ArchUnit 규칙 완화 | 변경 없음 | 레이어 경계 보장이 의미 없어짐 |

## 트레이드오프

**감수한 것들**

- `coffeeshout.profanity.domain` 패키지가 `jakarta.persistence`에 의존한다.
  순수 도메인 모델(persistence ignorance)을 지향한다면 JPA 어노테이션은 infra 관심사다.
  그러나 두 클래스는 비즈니스 로직이 없는 단순 상태 저장 객체이므로,
  JPA를 분리해서 얻는 이득이 변환 코드 비용보다 크지 않다고 판단했다.
- 도메인 단위 테스트에서 `@Entity` 클래스를 직접 사용하게 된다.
  JPA 컨텍스트 없이 인스턴스를 만들 수 있으므로(protected 생성자가 있어도 Reflection 우회 가능) 단위 테스트에 지장은 없다.

**얻은 것들**

- 매핑 코드 없이 ArchUnit 위반이 해소된다.
- 포트 인터페이스(`NicknameAuditRepository`, `NicknameFeedbackRepository`)가 domain 타입만 노출하게 된다.
- `NicknameAuditEntity`, `NicknameFeedbackEntity`라는 "Entity" 접미사가 사라져 application 레이어에서 읽는 코드가 더 자연스러워진다.

## 결과

- `NicknameAuditEntity` → `NicknameAudit` / `NicknameFeedbackEntity` → `NicknameFeedback`으로 이름 변경
- 두 클래스를 `infra.persistence.audit` → `domain.audit`으로 이전
- 참조하는 모든 클래스(application, infra, admin, room)의 import 갱신
- JPQL 쿼리 내 클래스 참조(`NicknameAuditEntity` → `NicknameAudit` 등) 갱신
- ADR 0018의 `infra/` 구조 설명 중 `persistence/audit/` 하위 항목은 이번 결정으로 무효화됐다
