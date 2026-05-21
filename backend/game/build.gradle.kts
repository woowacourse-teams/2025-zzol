// :game — 6게임 + orchestration (이전 minigame)

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val queryDslVersion = rootProject.extra["queryDsl"] as String

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
    implementation(project(":websocket"))
    implementation(project(":game-api"))
    implementation(project(":room"))

    annotationProcessor("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
}
