val googleGenAiVersion: String by rootProject.extra
val resilience4jVersion: String by rootProject.extra

dependencies {
    api(project(":global"))
    api(project(":room"))
    api(project(":game"))

    implementation("com.google.genai:google-genai:$googleGenAiVersion")
    implementation("com.github.jsqlparser:jsqlparser:5.0")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
}
