---
description: 프로덕션·테스트 코드 공통 Java 규칙
paths:
  - "src/**/*.java"
---

# 공통 Java 코드 규칙

## Fully-Qualified Name 금지

클래스는 항상 import 후 짧은 타입명을 사용한다.

```java
// 금지
org.springframework.http.HttpMethod.GET

// 허용
import org.springframework.http.HttpMethod;
// ...
HttpMethod.GET
```

## final 변수

인스턴스 변수·지역 변수는 `final`로 선언한다. 매개변수는 제외한다.

## Simplicity First

요청된 것만 정확히 구현한다. 투기적 기능(미래에 필요할 것 같은 기능, 재사용성을 위한 추상화)은 추가하지 않는다. 200줄을 50줄로 줄일 수 있다면 다시 작성한다.

## Surgical Changes

요청 범위 밖의 코드는 건드리지 않는다. 기존 코드 스타일을 유지한다. 작업과 무관한 dead code는 그대로 둔다. 변경으로 인해 실제로 불필요해진 import·함수만 제거한다.
