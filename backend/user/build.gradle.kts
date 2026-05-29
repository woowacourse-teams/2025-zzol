// :user — User + Auth + Friend (OAuth2, JWT, Security)

plugins {
    `java-test-fixtures`
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val jjwtVersion = rootProject.extra["jjwt"] as String

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":web"))
    implementation(project(":websocket"))
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    val queryDslVersion = rootProject.extra["queryDsl"] as String
    annotationProcessor("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
    testImplementation(project(":test-support"))
}

tasks.test {
    systemProperty("test.db.name", "user_test")
    systemProperty("test.redis.db", "2")
}
