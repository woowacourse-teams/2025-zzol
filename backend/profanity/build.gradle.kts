// :profanity — 비속어 필터링 자체 모듈 (Aho-Corasick + DB 기반 단어 목록) + 닉네임 AI 검열

plugins {
    `java-test-fixtures`
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))

    testImplementation(project(":test-support"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.ahocorasick)

    implementation("io.micrometer:micrometer-core")
    implementation(libs.resilience4j)
    implementation(libs.google.genai)
    implementation(libs.spring.boot.jackson2)

    testFixturesImplementation(project(":common"))
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation(libs.querydsl.jpa)
    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jpa") })
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
}

// 골든셋 평가 러너(@Tag("golden")). 실제 Gemini를 불러 build/reports/golden/에 리포트를 쓴다.
// GEMINI_API_KEY가 없으면 건너뛴다
tasks.register<Test>("goldenTest") {
    description = "닉네임 검열 골든셋 평가"
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("golden") }
    outputs.upToDateWhen { false }
}
