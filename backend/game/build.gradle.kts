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
    implementation(project(":user"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:context-propagation")

    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jakarta") })
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testFixturesImplementation(project(":game-api"))
    testFixturesImplementation(project(":test-support"))
    testImplementation(project(":test-support"))
    testImplementation(testFixtures(project(":room")))
    // 테스트 컨텍스트가 coffeeshout 전체를 스캔하며 :room·:user 빈이 ProfanityChecker 구현체를 요구한다
    testImplementation(project(":profanity"))
}
