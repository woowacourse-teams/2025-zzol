import java.util.concurrent.TimeUnit

plugins {
    id("org.springframework.boot") version "3.5.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

val springBootVersion = "3.5.3"
val springDocVersion by extra("2.8.3")
val ociSdkVersion by extra("3.74.1")
val redissonVersion by extra("3.27.2")
val zxingVersion by extra("3.5.3")
val queryDslVersion by extra("5.0.0")
val googleGenAiVersion by extra("1.44.0")
val testcontainersVersion by extra("2.0.4")
val reflectionsVersion by extra("0.10.2")
val resilience4jVersion by extra("2.2.0")
val jjwtVersion by extra("0.12.6")

allprojects {
    group = "coffeeshout"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    extensions.getByType<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
    }

    configurations {
        "compileOnly" {
            extendsFrom(configurations["annotationProcessor"])
        }
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "annotationProcessor"("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
        "annotationProcessor"("jakarta.annotation:jakarta.annotation-api")
        "annotationProcessor"("jakarta.persistence:jakarta.persistence-api")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.register<Exec>("pruneStaleTestContainers") {
        group = "verification"
        description = "테스트 시작 전 이전 실행에서 남은 TestContainers 컨테이너를 정리한다"
        commandLine(
            "docker", "container", "prune", "-f",
            "--filter", "label=org.testcontainers=true"
        )
        isIgnoreExitValue = true
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        dependsOn("pruneStaleTestContainers")
        useJUnitPlatform()
        exclude("**/QueryPerformanceTest.class")
        systemProperty("updateFixture", System.getProperty("updateFixture", "false"))
    }
}

tasks.register("generateCtags") {
    group = "build"
    description = "Universal Ctags로 Java 심볼 인덱스(tags 파일)를 생성한다"
    onlyIf { System.getenv("CI") == null }

    val sourceDirs: List<String> = project.subprojects.flatMap { p ->
        listOf(
            p.file("src/main/java").absolutePath,
            p.file("src/test/java").absolutePath
        )
    }
    val workDir: File = project.projectDir

    inputs.files(
        layout.projectDirectory.asFileTree.matching {
            include("**/src/main/java/**/*.java", "**/src/test/java/**/*.java")
            exclude("**/build/**")
        }
    )
    outputs.file(layout.projectDirectory.file("tags"))
    doLast {
        val existingDirs = sourceDirs.filter { java.io.File(it).exists() }
        if (existingDirs.isEmpty()) {
            logger.warn("ctags: 소스 디렉토리를 찾을 수 없습니다")
            return@doLast
        }

        val process: Process
        try {
            process = ProcessBuilder(
                listOf("ctags", "--languages=Java", "--fields=+n", "--extras=+q", "-R", "-f", "tags") + existingDirs
            )
                .directory(workDir)
                .start()
        } catch (e: java.io.IOException) {
            logger.warn("ctags를 찾을 수 없어 tags 파일 생성을 건너뜁니다: ${e.message}")
            return@doLast
        }

        try {
            val finished = process.waitFor(10L, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                logger.warn("ctags가 10초 내에 완료되지 않아 강제 종료했습니다")
            } else if (process.exitValue() != 0) {
                val stderr = process.errorStream.bufferedReader().readText().trim()
                logger.warn("ctags가 비정상 종료했습니다 (exit=${process.exitValue()}): $stderr")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn("ctags 대기 중 인터럽트가 발생했습니다: ${e.message}")
        }
    }
}
