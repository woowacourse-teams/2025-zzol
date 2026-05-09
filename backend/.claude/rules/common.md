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
