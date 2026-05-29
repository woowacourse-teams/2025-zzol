// :game — 6게임 + orchestration (이전 minigame)

plugins {
    `java-test-fixtures`
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val queryDslVersion = rootProject.extra["queryDsl"] as String

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":web"))
    implementation(project(":websocket"))
    implementation(project(":game-api"))
    implementation(project(":room"))
    implementation(project(":user"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("io.micrometer:micrometer-core")

    annotationProcessor("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testFixturesImplementation(project(":game-api"))
    testImplementation(project(":test-support"))
    testImplementation(testFixtures(project(":room")))
}

tasks.test {
    systemProperty("test.db.name", "game_test")
    systemProperty("test.redis.db", "4")
}
