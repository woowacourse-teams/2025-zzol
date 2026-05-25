plugins {
    id("org.springframework.boot") version "3.5.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "coffeeshout"
version = "0.0.1-SNAPSHOT"

// 공유 버전 변수 — 서브모듈이 rootProject.extra["key"]로 참조
val versions = mapOf(
    "springDoc"     to "2.8.3",
    "ociSdk"        to "3.74.1",
    "redisson"      to "3.27.2",
    "zxing"         to "3.5.3",
    "queryDsl"      to "5.0.0",
    "googleGenAi"   to "1.44.0",
    "testcontainers" to "2.0.5",
    "reflections"   to "0.10.2",
    "resilience4j"  to "2.2.0",
    "jjwt"          to "0.12.6",
)
versions.forEach { (k, v) -> extra[k] = v }

tasks.register<Exec>("pruneStaleTestContainers") {
    group = "verification"
    description = "종료된 Testcontainers 컨테이너를 제거한다. reuse 캐시 초기화 시 사용."
    commandLine("docker", "container", "prune", "-f", "--filter", "label=org.testcontainers=true")
    isIgnoreExitValue = true
}

// 모든 서브프로젝트 공통 설정
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    // Spring Boot BOM이 testcontainers 코어를 1.x로 다운그레이드하지 못하도록 오버라이드
    extra["testcontainers.version"] = rootProject.extra["testcontainers"] as String

    // Spring Boot bootJar 기본 비활성화 (라이브러리 모듈은 jar만, :app이 override)
    tasks.named("bootJar") { enabled = false }
    tasks.named("jar") { enabled = true }

    group = "coffeeshout"
    version = rootProject.version

    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion = JavaLanguageVersion.of(21) }
    }

    repositories {
        mavenCentral()
    }

    configurations {
        named("compileOnly") { extendsFrom(configurations["annotationProcessor"]) }
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        exclude("**/QueryPerformanceTest.class")
        systemProperty("updateFixture", System.getProperty("updateFixture", "false"))
        jvmArgs("-Xmx1g", "-XX:+HeapDumpOnOutOfMemoryError")
    }
}
