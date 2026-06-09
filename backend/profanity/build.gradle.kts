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

    testFixturesImplementation(project(":common"))
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation(libs.querydsl.jpa)
    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jpa") })
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
}
