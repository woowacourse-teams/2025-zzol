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

// pre-push 훅 자동 활성화 — 클론 후 별도 지식 없이 켜지게 한다 (#1659).
// 프론트만 만지는 사람은 frontend/package.json 의 prepare 스크립트가 같은 일을 한다.
tasks.register<Exec>("installGitHooks") {
    group = "build setup"
    description = "core.hooksPath 를 .githooks 로 설정해 pre-push 훅을 켠다."
    commandLine("git", "config", "core.hooksPath", ".githooks")
    isIgnoreExitValue = true // git 이 없거나 저장소가 아니어도 빌드를 막지 않는다
}

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
    // :test-support의 main과 testFixtures는 프로덕션이 아니라 테스트 인프라이므로 테스트 룰셋을 적용한다.
    //
    // 태스크를 이름으로 하나씩 집지 않고 withType으로 거는 이유 — PMD 플러그인은 소스셋마다 태스크를
    // 자동 생성한다(java-test-fixtures를 쓰는 모듈은 pmdTestFixtures가 생긴다). 위에서 기본 룰셋을 껐으므로
    // 지정에서 빠진 태스크는 룰셋이 0개가 되고, PMD는 그 상태로 "No rulesets specified"를 내며 죽는다.
    val mainRuleSet = if (name == "test-support") "ruleset-test.xml" else "ruleset-main.xml"
    tasks.withType<Pmd>().configureEach {
        val ruleSet = if (name == "pmdMain") mainRuleSet else "ruleset-test.xml"
        ruleSetFiles = files(rootProject.file("config/pmd/$ruleSet"))
    }

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

    tasks.named("build") {
        dependsOn(rootProject.tasks.named("installGitHooks"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        exclude("**/QueryPerformanceTest.class")
        systemProperty("updateFixture", System.getProperty("updateFixture", "false"))
        jvmArgs("-Xmx1g", "-XX:+HeapDumpOnOutOfMemoryError")
        // reuse-off로 JVM(모듈)마다 독립 컨테이너를 쓰므로 모듈별 DB/Redis 인덱스 격리는 불필요(이슈 #1402)
    }
}
