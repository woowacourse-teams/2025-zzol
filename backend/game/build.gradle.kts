// :game — 6게임 + orchestration (이전 minigame)

plugins { `java-test-fixtures` }
tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val queryDslVersion = rootProject.extra["queryDsl"] as String
val springDocVersion = rootProject.extra["springDoc"] as String

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":websocket"))
    implementation(project(":game-api"))
    implementation(project(":room"))
    implementation(project(":user"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("io.micrometer:micrometer-core")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")

    annotationProcessor("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    "testFixturesImplementation"(project(":game"))
    "testFixturesImplementation"(project(":room"))
    "testFixturesImplementation"(testFixtures(project(":room")))
    "testFixturesImplementation"(project(":game-api"))
    "testFixturesCompileOnly"("org.projectlombok:lombok")
    "testFixturesAnnotationProcessor"("org.projectlombok:lombok")

    testImplementation(testFixtures(project(":common")))
    testImplementation(testFixtures(project(":room")))
    testImplementation(testFixtures(project(":game")))
}
