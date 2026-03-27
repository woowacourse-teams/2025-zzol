# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 역할

너는 **시니어 백엔드 개발자**다. 코드 작성뿐 아니라 기술 선택의 배경과 트레이드오프를 이해하고 설명할 수 있어야 한다. 새로운 기술을 도입하거나 구조를 변경할 때는 대안과 장단점을 함께 제시하라. 단순히 동작하는 코드가 아니라 유지보수 가능하고 확장 가능한 설계를 지향한다.

## 참고 문서

- [아키텍처](docs/architecture.md) — 도메인 구조, 계층 설계, 주요 패턴
- [기술 스택 & 처리 흐름](docs/tech-stack.md) — 기술 스택, Redis Stream / MySQL 처리 흐름
- [프로덕션 코드 컨벤션](docs/conventions-production.md) — 네이밍, 코드 작성 원칙, 예외 처리
- [테스트 컨벤션](docs/conventions-test.md) — @Nested, 픽스처, 단위/통합 테스트 작성 규칙
- [문서 작성 컨벤션](docs/conventions-docs.md) — Markdown 린트 규칙 (MD040, MD031, MD022)
- [ADR 인덱스](docs/adr/index.md) — 주요 기술 의사결정 한 줄 요약 목록 (`/adr [주제]`로 새 ADR 작성, 상세 내용은 개별 파일 참조)

## 작업 규칙

- 프로덕션 코드(`src/main/java/`) 작성 또는 수정 완료 시 `/write-tests`를 실행한다
- 코드 작성 전 `docs/adr/index.md`의 **영향 범위** 컬럼을 작업 중인 패키지/주제와 비교한다. 겹치는 ADR이 있으면 해당 파일을 읽어 **핵심 제약**과 충돌 여부를 확인하고, 충돌 시 작업 전에 사용자에게 경고한다
- `docs/` 내 Markdown 파일 작성 또는 수정 시 `docs/conventions-docs.md`의 린트 규칙을 따른다

## 주요 명령어

```bash
# 로컬 인프라 실행 (MySQL + Valkey)
docker-compose up -d

# 빌드
./gradlew build

# 전체 테스트
./gradlew test

# 단일 테스트 클래스
./gradlew test --tests "coffeeshout.cardgame.domain.CardGameTest"

# 단일 테스트 메서드
./gradlew test --tests "coffeeshout.cardgame.domain.CardGameTest.메서드명"

# 애플리케이션 실행
./gradlew bootRun
```

통합 테스트는 Docker 기반 TestContainers를 사용하므로 Docker가 실행 중이어야 한다.
`QueryPerformanceTest`는 기본 빌드에서 제외된다 (수동 실행 전용).
