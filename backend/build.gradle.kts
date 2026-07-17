import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.spring.boot) apply false
}

group = "coffeeshout"
version = "0.0.1-SNAPSHOT"

val springBootVersion: String = libs.versions.spring.boot.get()

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
    apply(plugin = "jacoco")

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
        for (configurationName in listOf("implementation", "compileOnly", "annotationProcessor", "testCompileOnly", "testAnnotationProcessor", "developmentOnly")) {
            add(configurationName, platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
        }
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    pluginManager.withPlugin("java-test-fixtures") {
        dependencies {
            "testFixturesImplementation"(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
        }
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(false)
        }
        // event 패키지의 *Event record는 로직 없는 전송 DTO — 커버리지 측정 제외
        classDirectories.setFrom(
            classDirectories.files.map { dir ->
                fileTree(dir) { exclude("**/event/*Event.class") }
            }
        )
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        exclude("**/QueryPerformanceTest.class")
        systemProperty("updateFixture", System.getProperty("updateFixture", "false"))
        jvmArgs("-Xmx1g", "-XX:+HeapDumpOnOutOfMemoryError")
        // reuse-off로 JVM(모듈)마다 독립 컨테이너를 쓰므로 모듈별 DB/Redis 인덱스 격리는 불필요(이슈 #1402)
    }
}
