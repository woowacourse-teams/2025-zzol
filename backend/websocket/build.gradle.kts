// :websocket — STOMP 플랫폼 (도메인 무지)

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))
    implementation(project(":web"))

    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-observation")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.micrometer:context-propagation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.spring.boot.jackson2)

    testImplementation(project(":test-support"))
    // Boot 4 모듈러 테스트 스타터 — @AutoConfigureMockMvc/TestRestTemplate이 각각 별도 모듈로 분리됨
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-restclient")
}
