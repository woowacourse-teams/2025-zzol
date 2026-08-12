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
    // Boot 4는 오토컨피그를 기술별 모듈로 분리했다(테스트 스타터도 아래 36행처럼 분리됨).
    // flyway-core/-mysql는 라이브러리일 뿐이라, spring-boot-flyway(오토컨피그 모듈)가 없으면
    // FlywayMigrationInitializer 빈이 생성되지 않아 마이그레이션이 조용히 실행되지 않는다(#1606).
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("com.mysql:mysql-connector-j")

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

    // Flyway 검증 테스트(FlywayMigrationIntegrationTest)가 전 마이그레이션 체인을 실제로 돌리며,
    // 그중 V34(OAuth 이메일 백필)가 이 두 키를 System.getenv로 요구한다(운영은 배포 시크릿).
    // 빈 DB엔 백필할 행이 없어 키는 SHA-256 파생을 통과할 길이(32자 이상)면 충분하다.
    // 다른 테스트는 Flyway를 꺼두어 이 변수를 읽지 않으므로 태스크 전역 설정이 무해하다.
    environment("USER_EMAIL_ENCRYPTION_KEY", "test-only-email-encryption-key-000000000000")
    environment("USER_EMAIL_HMAC_KEY", "test-only-email-hmac-key-00000000000000000000")
}
