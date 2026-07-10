// :game — 6게임 + orchestration (이전 minigame)

plugins {
    `java-test-fixtures`
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":web"))
    implementation(project(":websocket"))
    implementation(project(":game-api"))
    implementation(project(":room"))
    // :user 프로덕션 의존은 제거됨(이슈 #1547) — 유저 통계는 MiniGameStatsRecordedEvent(:game-api)
    // 발행 → :user 구독으로 역전했다. 다만 :user는 :room을 통해 런타임 classpath엔 전이 포함되어
    // @SpringBootTest 전체 스캔 시 로드되므로, 테스트 mock(ExternalPortMockConfig) 컴파일용으로만
    // testImplementation 을 남긴다.

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:context-propagation")

    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jpa") })
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testFixturesImplementation(project(":game-api"))
    testFixturesImplementation(project(":test-support"))
    testImplementation(project(":test-support"))
    testImplementation(testFixtures(project(":room")))
    // 테스트 컨텍스트가 coffeeshout 전체를 스캔하며 :room·:user 빈이 ProfanityChecker 구현체를 요구한다
    testImplementation(project(":profanity"))
    // 프로덕션 의존은 아니나(위 참조), 전체 스캔 컨텍스트의 :user 빈 mock 등록용으로 테스트 스코프만 유지
    testImplementation(project(":user"))
}
