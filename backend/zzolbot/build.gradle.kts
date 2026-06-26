// :zzolbot — 운영자 AI 어시스턴트 (향후 독립 배포 가능)

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

// 자동 수정 코딩 에이전트 CLI. GitHub Actions 워커에서 실행한다(앱 컨테이너 밖).
//   ./gradlew :zzolbot:proposePatch --args="<inputJson> <repoRoot> <outputJson>"
tasks.register<JavaExec>("proposePatch") {
    group = "zzolbot"
    description = "스택트레이스로 결함을 특정하고 Gemini로 수정·재현 테스트를 제안한다(JSON 출력)."
    mainClass.set("coffeeshout.zzolbot.remediation.agent.RemediationAgentMain")
    classpath = sourceSets["main"].runtimeClasspath
}

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":web"))
    implementation(project(":game-api"))
    implementation(project(":room"))
    implementation(project(":game"))

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
