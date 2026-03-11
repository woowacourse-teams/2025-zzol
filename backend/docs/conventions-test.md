# 테스트 컨벤션

## 작성 원칙

- 테스트 메서드명은 한글로 작성한다 (도메인 언어 사용)
- `@Nested`로 연관된 테스트를 시나리오 단위로 그룹화한다. 중첩 클래스명은 테스트 대상 상황을 설명한다
- 복수 검증은 `SoftAssertions`를 사용한다
- `@SpringBootTest` 없이 순수 Java로 작성한다 (통합 테스트 제외)

## 픽스처

`src/test/java/coffeeshout/fixture/`에 픽스처 클래스를 모아 관리한다. 테스트 데이터를 직접 생성하지 않고 픽스처를 통해 재사용한다. 메서드명은 한글 도메인 용어를 사용한다.

## 통합 테스트 (WebSocket)

`WebSocketIntegrationTestSupport`를 상속하여 실제 STOMP 세션으로 테스트한다. `assertMessage`는 JSONAssert(lenient mode)로 비교한다.

## 테스트 프로파일

통합 테스트는 `test` 프로파일을 사용하며 `application-test.yml`이 자동 적용된다.
- 타이밍 값이 500ms~2s로 단축됨
- DB: H2 인메모리 사용 (Flyway 비활성화)
- Valkey(Redis): TestContainers로 실제 컨테이너 구동 (`TestContainerConfig`)
- Redisson 제외
