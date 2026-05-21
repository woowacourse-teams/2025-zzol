// :game-api — 게임 SPI(Service Provider Interface) 및 공유 타입 모음
//
// ※ "api"는 HTTP REST API가 아닌 Java 관례의 "공개 인터페이스 모음"을 의미합니다.
//   (slf4j-api, opentelemetry-api와 같은 용법)
//
// 이 모듈이 포함하는 것:
//   - Playable, MiniGameFactory  : 게임 구현체가 반드시 따라야 할 SPI
//   - MiniGameScore/Result/Type  : room·game 모듈이 공유하는 게임 도메인 타입
//   - FlowScheduler/Handle       : 게임 플로우 추상화 (DAG 스타일 단계 진행)
//   - PlayerView                 : 게임이 플레이어를 추상화해서 보는 인터페이스
//   - MiniGameStarted/Finished   : 게임 생명주기 이벤트
//
// 새 게임을 추가할 때: 이 모듈의 SPI를 구현하고 :game 모듈에 위치시킵니다.

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))
    implementation(project(":infra"))

    implementation("io.micrometer:micrometer-core")
}
