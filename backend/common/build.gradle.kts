// :common — 순수 추상 (Spring 무관)
// exception 계층, 이벤트 계약, VO, 닉네임 유틸
// SLF4J(로깅 파사드)·Jackson(이벤트 직렬화 어노테이션)은 Spring-free 표준 의존성으로 허용

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    // BaseEvent의 @JsonTypeInfo 때문에 Jackson 어노테이션 필요
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    // NotificationMarker의 SLF4J Marker 때문에 필요
    implementation("org.slf4j:slf4j-api")
}
