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
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation(libs.resilience4j)

    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jpa") })
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testFixturesImplementation(project(":game-api"))
    testImplementation(project(":test-support"))
    testImplementation(testFixtures(project(":game")))
    testImplementation(testFixtures(project(":profanity")))
    testImplementation(libs.archunit)
}
