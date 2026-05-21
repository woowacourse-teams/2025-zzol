// :admin — 운영자 영역 (dashboard + patchnote + report)

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val resilience4jVersion = rootProject.extra["resilience4j"] as String
val queryDslVersion = rootProject.extra["queryDsl"] as String

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":user"))
    implementation(project(":room"))
    implementation(project(":game-api"))
    implementation(project(":game"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    val redissonVersion = rootProject.extra["redisson"] as String
    implementation("org.redisson:redisson-spring-boot-starter:$redissonVersion")

    val queryDslVersion2 = rootProject.extra["queryDsl"] as String
    implementation("com.querydsl:querydsl-jpa:$queryDslVersion2:jakarta")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")

    val springDocVersion = rootProject.extra["springDoc"] as String
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    annotationProcessor("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
}
