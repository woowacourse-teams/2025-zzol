// :room — Room aggregate + Player + Roulette + RoomSessionToken

plugins {
    `java-test-fixtures`
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val ociSdkVersion = rootProject.extra["ociSdk"] as String
val zxingVersion = rootProject.extra["zxing"] as String
val jjwtVersion = rootProject.extra["jjwt"] as String
val queryDslVersion = rootProject.extra["queryDsl"] as String

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":web"))
    implementation(project(":websocket"))
    implementation(project(":user"))
    implementation(project(":game-api"))

    implementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:$ociSdkVersion"))
    implementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey3")

    implementation("com.google.zxing:core:$zxingVersion")
    implementation("com.google.zxing:javase:$zxingVersion")

    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:context-propagation")

    val resilience4jVersion = rootProject.extra["resilience4j"] as String
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
    annotationProcessor("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testFixturesImplementation(project(":game-api"))
    testFixturesImplementation("org.springframework.boot:spring-boot-test")
    testImplementation(project(":test-support"))
    testImplementation(testFixtures(project(":profanity")))
    testImplementation(testFixtures(project(":user")))
    testImplementation(project(":profanity"))
}
