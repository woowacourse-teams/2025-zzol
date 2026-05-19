val redissonVersion: String by rootProject.extra
val queryDslVersion: String by rootProject.extra
val zxingVersion: String by rootProject.extra
val reflectionsVersion: String by rootProject.extra
val resilience4jVersion: String by rootProject.extra
val testcontainersVersion: String by rootProject.extra

dependencies {
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-aop")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    api("org.redisson:redisson-spring-boot-starter:$redissonVersion")

    api("com.querydsl:querydsl-jpa:$queryDslVersion:jakarta")

    api("com.google.zxing:core:$zxingVersion")
    api("com.google.zxing:javase:$zxingVersion")

    api("io.micrometer:micrometer-registry-prometheus")
    api("io.micrometer:micrometer-observation")
    api("io.micrometer:micrometer-tracing-bridge-otel")
    api("io.opentelemetry:opentelemetry-exporter-otlp")
    api("io.micrometer:context-propagation")

    implementation("org.reflections:reflections:$reflectionsVersion")
    api("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
    implementation("org.jspecify:jspecify")

    testImplementation("com.h2database:h2")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-mysql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")
    testImplementation("io.micrometer:micrometer-tracing-test")
}
