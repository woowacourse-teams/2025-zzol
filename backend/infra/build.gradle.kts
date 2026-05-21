// :infra — Spring + JPA + Redis + Outbox + Lock + IpBlock + Health + Metric

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val redissonVersion = rootProject.extra["redisson"] as String
val queryDslVersion = rootProject.extra["queryDsl"] as String
val reflectionsVersion = rootProject.extra["reflections"] as String

dependencies {
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.redisson:redisson-spring-boot-starter:$redissonVersion")

    implementation("com.querydsl:querydsl-jpa:$queryDslVersion:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-observation")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.micrometer:context-propagation")

    val springDocVersion = rootProject.extra["springDoc"] as String
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")

    implementation("org.reflections:reflections:$reflectionsVersion")
}
