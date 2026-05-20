val ociSdkVersion: String by rootProject.extra
val jjwtVersion: String by rootProject.extra
val testcontainersVersion: String by rootProject.extra

dependencies {
    api(project(":global"))
    api(project(":websocket"))

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:$ociSdkVersion"))
    implementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3")

    api("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    implementation("io.github.vaneproject:badwordfiltering:1.0.0")

    testImplementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-mysql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")
}
