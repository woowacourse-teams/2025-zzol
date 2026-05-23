// :profanity — 비속어 필터링 자체 모듈 (Aho-Corasick + DB 기반 단어 목록)

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))
}
