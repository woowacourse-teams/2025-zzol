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

    implementation("org.springframework.boot:spring-boot-starter-web")

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
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-aop")
    testImplementation("io.micrometer:micrometer-core")
    val ociSdkVersion = rootProject.extra["ociSdk"] as String
    testImplementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:$ociSdkVersion"))
    testImplementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage")
    testImplementation("com.oracle.oci.sdk:oci-java-sdk-common")
    testImplementation("io.github.vaneproject:badwordfiltering:1.0.0")
    val resilience4jVersion = rootProject.extra["resilience4j"] as String
    testImplementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")

    val googleGenAiVersion = rootProject.extra["googleGenAi"] as String
    testImplementation("com.google.genai:google-genai:$googleGenAiVersion")

    val redissonVersion = rootProject.extra["redisson"] as String
    testImplementation("org.redisson:redisson-spring-boot-starter:$redissonVersion")

    val jjwtVersion = rootProject.extra["jjwt"] as String
    testImplementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    testRuntimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // ArchUnit — 아키텍처 규칙 테스트
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}
