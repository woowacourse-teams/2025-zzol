plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "coffeeshout"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    enabled = false
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val springDocVersion = "2.8.3"
val ociSdkVersion = "3.74.1"
val redissonVersion = "3.27.2"
val zxingVersion = "3.5.3"
val queryDslVersion = "5.0.0"
val websocketDocsVersion = "1.0.7"
val googleGenAiVersion = "1.44.0"
val testcontainersVersion = "2.0.4"
val reflectionsVersion = "0.10.2"
val resilience4jVersion = "2.2.0"
val jjwtVersion = "0.12.6"

dependencies {
    // --- Spring Boot Starters (버전 생략: Boot가 관리) ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // --- Database & Migration ---
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("com.mysql:mysql-connector-j")

    // Redisson (Boot가 관리하지 않음 -> 변수 사용)
    implementation("org.redisson:redisson-spring-boot-starter:${redissonVersion}")

    // --- QueryDSL ---
    // Jakarta 분류가 필요하므로 버전 명시가 안전할 수 있음
    implementation("com.querydsl:querydsl-jpa:${queryDslVersion}:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:${queryDslVersion}:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // --- Utils ---
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    developmentOnly("me.paulschwarz:spring-dotenv:4.0.0")
    implementation("io.github.20hyeonsulee:websocket-docs-generator:${websocketDocsVersion}")

    implementation("com.google.zxing:core:${zxingVersion}")
    implementation("com.google.zxing:javase:${zxingVersion}")

    // --- Oracle Cloud Infrastructure (BOM 활용) ---
    implementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:${ociSdkVersion}"))
    implementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${springDocVersion}")

    // --- Metrics ---
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-observation")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.micrometer:context-propagation")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // --- Test ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.micrometer:micrometer-tracing-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2")
    testImplementation("org.testcontainers:testcontainers:${testcontainersVersion}")
    testImplementation("org.testcontainers:testcontainers-mysql:${testcontainersVersion}")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:${testcontainersVersion}")
    // --- Reflections (클래스패스 스캔) ---
    implementation("org.reflections:reflections:${reflectionsVersion}")

    // --- Resilience4j (서킷 브레이커, 리트라이) ---
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${resilience4jVersion}")

    // --- 비속어 필터 ---
    implementation("io.github.vaneproject:badwordfiltering:1.0.0")

    // --- Gemini AI ---
    implementation("com.google.genai:google-genai:${googleGenAiVersion}")

    // --- 운영자 대시보드 ---
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")

    // --- OAuth2 / JWT ---
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("io.jsonwebtoken:jjwt-api:${jjwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${jjwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${jjwtVersion}")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.register("generateCtags") {
    group = "build"
    description = "Universal Ctags로 Java 심볼 인덱스(tags 파일)를 생성한다"
    onlyIf { System.getenv("CI") == null }
    inputs.dir("src/main/java")
    inputs.dir("src/test/java")
    outputs.file("tags")
    val workDir = projectDir
    doLast {
        val process: Process
        try {
            process = ProcessBuilder(
                "ctags",
                "--languages=Java",
                "--fields=+n",
                "--extras=+q",
                "-R",
                "-f", "tags",
                "src/main/java",
                "src/test/java"
            )
                .directory(workDir)
                .start()
        } catch (e: java.io.IOException) {
            logger.warn("ctags를 찾을 수 없어 tags 파일 생성을 건너뜁니다: ${e.message}")
            return@doLast
        }

        try {
            val finished = process.waitFor(10L, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                logger.warn("ctags가 10초 내에 완료되지 않아 강제 종료했습니다")
            } else if (process.exitValue() != 0) {
                val stderr = process.errorStream.bufferedReader().readText().trim()
                logger.warn("ctags가 비정상 종료했습니다 (exit=${process.exitValue()}): $stderr")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn("ctags 대기 중 인터럽트가 발생했습니다: ${e.message}")
        }
    }
}

tasks.named("compileJava") {
    finalizedBy("generateCtags")
}

tasks.register<Exec>("pruneStaleTestContainers") {
    group = "verification"
    description = "테스트 시작 전 이전 실행에서 남은 TestContainers 컨테이너를 정리한다"
    commandLine(
        "docker", "container", "prune", "-f",
        "--filter", "label=org.testcontainers=true"
    )
    isIgnoreExitValue = true
}

tasks.withType<Test> {
    dependsOn("pruneStaleTestContainers")
    useJUnitPlatform()
    // 성능 테스트는 CI에서 제외 (수동 실행용)
    exclude("**/QueryPerformanceTest.class")
}
