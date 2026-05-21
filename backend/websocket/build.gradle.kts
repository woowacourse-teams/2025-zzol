// :websocket — STOMP 플랫폼 (도메인 무지)

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))

    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-observation")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.micrometer:context-propagation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    val springDocVersion = rootProject.extra["springDoc"] as String
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")

    val jjwtVersion = rootProject.extra["jjwt"] as String
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    testImplementation(testFixtures(project(":common")))
    testImplementation(project(":room"))
    testImplementation(testFixtures(project(":room")))
}
