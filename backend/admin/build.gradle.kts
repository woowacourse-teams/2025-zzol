// :admin — 운영자 영역 (dashboard + patchnote + report)

plugins {
    `java-test-fixtures`
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":web"))
    implementation(project(":user"))
    implementation(project(":room"))
    implementation(project(":profanity"))
    implementation(project(":game-api"))
    implementation(project(":game"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(libs.redisson)
    implementation(libs.querydsl.jpa)

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    // 관리자 토큰 발급/검증. :user 가 쓰는 jjwt 는 그쪽 implementation 이라 여기로 전파되지 않는다.
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    // Boot 4에서 RestClient 자동구성이 별도 모듈로 분리 — OAuth2 client가 RestClient.Builder를 요구
    implementation("org.springframework.boot:spring-boot-restclient")
    implementation(libs.resilience4j)

    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jpa") })
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testFixturesImplementation(project(":game-api"))
    testImplementation(project(":test-support"))
    testImplementation(testFixtures(project(":game")))
    testImplementation(testFixtures(project(":profanity")))
    testImplementation(libs.archunit)
    // Boot 4 모듈러 테스트 스타터 — @AutoConfigureMockMvc/@DataJpaTest/@AutoConfigureTestDatabase가 각각 별도 모듈로 분리됨
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-jdbc-test")
}
