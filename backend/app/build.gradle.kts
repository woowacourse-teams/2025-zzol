// :app — Spring Boot 진입점, application.yml, Flyway 마이그레이션

// 루트의 subprojects{}가 bootJar=false로 설정했으므로 :app만 다시 활성화
tasks.named("bootJar") { enabled = true }
tasks.named("jar") { enabled = false }

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":websocket"))
    implementation(project(":game-api"))
    implementation(project(":user"))
    implementation(project(":room"))
    implementation(project(":game"))
    implementation(project(":admin"))
    implementation(project(":zzolbot"))

    // --- Database & Migration ---
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("com.mysql:mysql-connector-j")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    developmentOnly("me.paulschwarz:spring-dotenv:4.0.0")

    val testcontainersVersion = rootProject.extra["testcontainers"] as String
    testImplementation("com.h2database:h2")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-mysql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")
    testImplementation("io.micrometer:micrometer-tracing-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
}
