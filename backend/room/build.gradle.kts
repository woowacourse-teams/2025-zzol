val ociSdkVersion: String by rootProject.extra
val springDocVersion: String by rootProject.extra

dependencies {
    api(project(":global"))
    api(project(":websocket"))
    api(project(":user"))

    implementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:$ociSdkVersion"))
    implementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3")

    implementation("io.github.vaneproject:badwordfiltering:1.0.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
}
