// :user — User + Auth + Friend (OAuth2, JWT, Security)

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
    // 미니게임 결과 이벤트(MiniGameStatsRecordedEvent) 구독 — :game→:user 의존 역전
    implementation(project(":game-api"))
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    // Boot 4에서 RestClient 자동구성이 별도 모듈로 분리 — OAuth2 client가 RestClient.Builder를 요구
    implementation("org.springframework.boot:spring-boot-restclient")
    implementation("io.micrometer:micrometer-core")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jpa") })
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.wiremock)
    testImplementation(project(":test-support"))
    // Boot 4 모듈러 테스트 스타터 — @AutoConfigureMockMvc가 별도 모듈로 분리됨
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
}
