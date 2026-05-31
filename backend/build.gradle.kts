plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

group = "coffeeshout"
version = "0.0.1-SNAPSHOT"

val testcontainersVersion: String = libs.versions.testcontainers.get()

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
    extra["testcontainers.version"] = testcontainersVersion

    // Spring Boot bootJar 기본 비활성화 (라이브러리 모듈은 jar만, :app이 override)
    tasks.named("bootJar") { enabled = false }
    tasks.named("jar") { enabled = true }

    group = "coffeeshout"
    version = rootProject.version

    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion = JavaLanguageVersion.of(21) }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
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

    // 모듈별 Redis DB 인덱스 — 새 모듈 추가 시 여기에 등록
    val redisDbByModule = mapOf(
        "app"       to 0,
        "room"      to 1,
        "user"      to 2,
        "websocket" to 3,
        "game"      to 4,
        "zzolbot"   to 5,
        "infra"     to 6,
    )

    tasks.withType<Test> {
        useJUnitPlatform()
        exclude("**/QueryPerformanceTest.class")
        systemProperty("updateFixture", System.getProperty("updateFixture", "false"))
        jvmArgs("-Xmx1g", "-XX:+HeapDumpOnOutOfMemoryError")

        // 모듈별 독립 DB/Redis — 병렬 테스트 실행 시 간섭 방지
        // :app은 전 모듈 통합 DB로 zzol_test 유지
        val dbName = if (project.name == "app") "zzol_test"
                     else "zzol_test_${project.name.replace("-", "_")}"
        require(dbName.matches(Regex("[a-zA-Z0-9_]+"))) {
            "Invalid test DB name '$dbName' for module '${project.name}': only alphanumeric and underscore allowed"
        }
        systemProperty("test.db.name", dbName)
        systemProperty("test.redis.db", redisDbByModule[project.name] ?: 0)
    }
}
