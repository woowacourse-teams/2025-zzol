import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spotless) apply false
}

group = "coffeeshout"
version = "0.0.1-SNAPSHOT"

val springBootVersion: String = libs.versions.spring.boot.get()
val pmdVersion: String = libs.versions.pmd.get()

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
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "pmd")

    // 구조 규칙은 PMD가 지적한다 (ADR-0036).
    extensions.configure<PmdExtension> {
        toolVersion = pmdVersion
        ruleSets = emptyList() // PMD 기본 룰셋 비활성 — 아래 룰셋에 명시한 규칙만 쓴다
        isConsoleOutput = true
    }

    // 룰셋은 소스셋별로 다르다 — 테스트의 @Autowired 필드 주입은 JUnit+Spring 관용이라 금지 대상이 아니고,
    // Thread.sleep·JUnit 단언 금지는 반대로 테스트에만 의미가 있다.
    // :test-support의 main은 프로덕션이 아니라 테스트 인프라이므로 테스트 룰셋을 적용한다.
    val mainRuleSet = if (name == "test-support") "ruleset-test.xml" else "ruleset-main.xml"
    tasks.named<Pmd>("pmdMain") { ruleSetFiles = files(rootProject.file("config/pmd/$mainRuleSet")) }
    tasks.named<Pmd>("pmdTest") { ruleSetFiles = files(rootProject.file("config/pmd/ruleset-test.xml")) }

    // 포맷은 Spotless가 자동 수정한다 (ADR-0036). ./gradlew spotlessApply
    extensions.configure<SpotlessExtension> {
        // 전체 재포맷 대신 origin/dev 이후 변경된 파일만 검사한다.
        // 기존 파일을 건드리지 않아 git blame·리뷰 diff가 오염되지 않는다.
        ratchetFrom("origin/dev")
        java {
            removeUnusedImports()
            palantirJavaFormat()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

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
