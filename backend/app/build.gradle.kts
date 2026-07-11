// :app — Spring Boot 진입점, application.yml, Flyway 마이그레이션

// 루트의 subprojects{}가 bootJar=false로 설정했으므로 :app만 다시 활성화
tasks.named("bootJar") { enabled = true }
tasks.named("jar") { enabled = false }

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":web"))
    implementation(project(":websocket"))
    implementation(project(":game-api"))
    implementation(project(":user"))
    implementation(project(":room"))
    implementation(project(":game"))
    implementation(project(":admin"))
    implementation(project(":zzolbot"))
    implementation(project(":profanity"))

    // --- Database & Migration ---
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("com.mysql:mysql-connector-j")

    // Jackson 2 호환 모드 — jjwt-jackson 등 Jackson 2 직접 의존 서드파티용 (ADR-0020 Phase 2, deprecated stop-gap)
    implementation(libs.spring.boot.jackson2)

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    developmentOnly(libs.spring.dotenv)

    testImplementation(project(":test-support"))
    testImplementation(testFixtures(project(":room")))
    testImplementation(testFixtures(project(":user")))
    testImplementation(testFixtures(project(":game")))
    testImplementation(testFixtures(project(":admin")))
    testImplementation(testFixtures(project(":profanity")))
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("io.micrometer:micrometer-tracing-test")
    // Boot 4 모듈러 테스트 스타터 — @AutoConfigureMockMvc/@AutoConfigureTracing이 각각 별도 모듈로 분리됨
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-micrometer-tracing-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.micrometer:micrometer-core")
    testImplementation(libs.resilience4j)
    testImplementation(libs.redisson)
    testImplementation(libs.archunit)
}

tasks.withType<Test> {
    // :app 통합 테스트는 Spring context를 여러 개 띄우므로 힙을 더 크게 잡는다
    jvmArgs("-Xmx2g", "-XX:MaxMetaspaceSize=512m")
}
