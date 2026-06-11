// :infra — Spring + JPA + Redis + Outbox + Lock + IpBlock + Health + Metric

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Ip 값 객체의 IPv4/IPv6 검증용 — Lettuce가 이미 runtime에 포함하므로 새 jar 추가 없음
    implementation("io.netty:netty-common")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation(libs.redisson)

    implementation(libs.querydsl.jpa)
    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jpa") })
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-observation")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.micrometer:context-propagation")

    implementation(libs.reflections)
    implementation(libs.resilience4j)

    testImplementation("io.micrometer:micrometer-tracing-test")
    testImplementation(project(":test-support"))
}
