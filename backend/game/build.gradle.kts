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
    implementation(project(":user"))

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
    // :game 프로덕션은 :room을 모르지만(FK ID 참조로 분리), 테스트는 Room/Player 도메인 픽스처를 사용한다.
    testImplementation(project(":room"))
    testImplementation(testFixtures(project(":room")))
    // 테스트 컨텍스트가 coffeeshout 전체를 스캔하며 :room·:user 빈이 ProfanityChecker 구현체를 요구한다
    testImplementation(project(":profanity"))
}
