// :zzolbot — 운영자 AI 어시스턴트 (향후 독립 배포 가능)

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":web"))
    implementation(project(":game-api"))
    implementation(project(":room"))

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation(libs.google.genai)
    implementation(libs.jsqlparser)
    implementation(libs.resilience4j)

    testImplementation(project(":test-support"))
    testImplementation(testFixtures(project(":room")))
    testImplementation(libs.wiremock)
}
