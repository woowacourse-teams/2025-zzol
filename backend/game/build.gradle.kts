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

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:context-propagation")
    implementation(libs.spring.boot.jackson2)

    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jpa") })
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testFixturesImplementation(project(":game-api"))
    testFixturesImplementation(project(":test-support"))
    testImplementation(project(":test-support"))
    testImplementation(testFixtures(project(":room")))
    // 테스트 컨텍스트가 coffeeshout 전체를 스캔하며 :room·:user 빈이 ProfanityChecker 구현체를 요구한다
    testImplementation(project(":profanity"))
    // :room·:user 프로덕션 의존은 제거됨(ADR-0034 / #1547) — 방·플레이어 id는 :game-api의 RoomSnapshotQuery
    // 포트(:room 구현)로, 유저 통계는 MiniGameStatsRecordedEvent 발행으로 역전했다. 다만 @SpringBootTest
    // 전체 스캔 컨텍스트가 두 모듈 빈(RoomSnapshotQueryAdapter·mock 대상 등)을 로드하므로 테스트 스코프로만 남긴다.
    testImplementation(project(":room"))
    testImplementation(project(":user"))
}
