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
    "testcontainers" to "2.0.4",
    "reflections"   to "0.10.2",
    "resilience4j"  to "2.2.0",
    "jjwt"          to "0.12.6",
)
versions.forEach { (k, v) -> extra[k] = v }

tasks.register("generateCtags") {
    group = "build"
    description = "Universal Ctags로 Java 심볼 인덱스(tags 파일)를 생성한다"
    onlyIf { System.getenv("CI") == null }
    outputs.file("tags")
    val workDir = projectDir
    doLast {
        val process: Process
        try {
            process = ProcessBuilder("ctags", "--languages=Java", "--fields=+n", "--extras=+q", "-R", "-f", "tags", "src", "common/src", "infra/src", "websocket/src", "game-api/src", "user/src", "room/src", "game/src", "admin/src", "zzolbot/src", "app/src")
                .directory(workDir).start()
        } catch (e: java.io.IOException) {
            logger.warn("ctags를 찾을 수 없어 tags 파일 생성을 건너뜁니다: ${e.message}")
            return@doLast
        }
        try {
            if (!process.waitFor(10L, TimeUnit.SECONDS)) process.destroyForcibly()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}

tasks.register<Exec>("pruneStaleTestContainers") {
    group = "verification"
    commandLine("docker", "container", "prune", "-f", "--filter", "label=org.testcontainers=true")
    isIgnoreExitValue = true
}

// 모든 서브프로젝트 공통 설정
subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

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
