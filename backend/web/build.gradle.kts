// :web — 공유 HTTP 인프라 (RestExceptionHandler, CORS, WebMvc 설정)

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val springDocVersion = rootProject.extra["springDoc"] as String

dependencies {
    implementation(project(":common"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
}
