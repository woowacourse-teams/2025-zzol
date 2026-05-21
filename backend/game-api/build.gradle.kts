// :game-api — 게임 SPI + 공통 추상 (OCP 핵심)
// Playable, MiniGameFactory, Score/Result/Type, FlowScheduler

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))
}
