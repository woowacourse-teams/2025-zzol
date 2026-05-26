// :profanity — 비속어 필터링 자체 모듈 (Aho-Corasick + DB 기반 단어 목록) + 닉네임 AI 검열

plugins {
    `java-test-fixtures`
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val googleGenAiVersion = rootProject.extra["googleGenAi"] as String

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))

    testImplementation(project(":test-support"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.ahocorasick:ahocorasick:0.6.3")

    implementation("io.micrometer:micrometer-core")

    val resilience4jVersion = rootProject.extra["resilience4j"] as String
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")

    implementation("com.google.genai:google-genai:$googleGenAiVersion")

    testFixturesImplementation(project(":common"))
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
