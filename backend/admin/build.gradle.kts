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
    // Boot 4에서 RestClient 자동구성이 별도 모듈로 분리 — OAuth2 client가 RestClient.Builder를 요구
    implementation("org.springframework.boot:spring-boot-restclient")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation(libs.resilience4j)
    implementation(libs.spring.boot.jackson2)

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
