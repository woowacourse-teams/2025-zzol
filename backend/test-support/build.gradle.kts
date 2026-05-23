// :test-support — 통합/서비스 테스트 공용 인프라 (TestContainers, 베이스 클래스, 픽스처 유틸)
// src/main/java에 위치해야 다른 모듈의 testImplementation으로 소비 가능
// java-library 플러그인으로 api 설정 사용 → 전이 의존성 노출

plugins {
    `java-library`
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val testcontainersVersion = rootProject.extra["testcontainers"] as String

dependencies {
    // 다른 모듈의 testImplementation에서 전이적으로 사용되도록 api로 선언
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-websocket")
    api("org.springframework.boot:spring-boot-starter-web")

    api("org.testcontainers:testcontainers:$testcontainersVersion")
    api("org.testcontainers:testcontainers-mysql:$testcontainersVersion")
    api("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")

    // ExceptionAssertions — CoffeeShoutException, ErrorCode
    api(project(":common"))

    // BaseServiceTestConfig / BaseIntegrationTestConfig — FlowScheduler 인터페이스 mock
    api(project(":game-api"))

    api("com.mysql:mysql-connector-j")
    api("com.h2database:h2")
}
