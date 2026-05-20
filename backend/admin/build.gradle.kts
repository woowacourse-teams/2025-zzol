val springDocVersion: String by rootProject.extra

dependencies {
    api(project(":global"))
    api(project(":websocket"))
    api(project(":room"))
    api(project(":game"))
    api(project(":user"))

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
}
