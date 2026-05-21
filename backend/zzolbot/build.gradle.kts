// :zzolbot — 운영자 AI 어시스턴트 (향후 독립 배포 가능)

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val googleGenAiVersion = rootProject.extra["googleGenAi"] as String
val resilience4jVersion = rootProject.extra["resilience4j"] as String

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":room"))

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("com.google.genai:google-genai:$googleGenAiVersion")
    implementation("com.github.jsqlparser:jsqlparser:5.0")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
}
