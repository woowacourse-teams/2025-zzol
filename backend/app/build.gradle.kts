plugins {
    id("org.springframework.boot")
}

val ociSdkVersion: String by rootProject.extra
val googleGenAiVersion: String by rootProject.extra
val testcontainersVersion: String by rootProject.extra

tasks.jar {
    enabled = false
}

tasks.named("compileJava") {
    finalizedBy(rootProject.tasks.named("generateCtags"))
}

dependencies {
    implementation(project(":global"))
    implementation(project(":websocket"))
    implementation(project(":user"))
    implementation(project(":room"))
    implementation(project(":game"))
    implementation(project(":admin"))
    implementation(project(":zzolbot"))

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("com.mysql:mysql-connector-j")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    developmentOnly("me.paulschwarz:spring-dotenv:4.0.0")

    testImplementation(testFixtures(project(":global")))

    testImplementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:$ociSdkVersion"))
    testImplementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage")
    testImplementation("com.oracle.oci.sdk:oci-java-sdk-common")
    testImplementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3")
    testImplementation("com.google.genai:google-genai:$googleGenAiVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-mysql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")
    testImplementation("io.micrometer:micrometer-tracing-test")
    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
    testImplementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("io.github.vaneproject:badwordfiltering:1.0.0")
}
