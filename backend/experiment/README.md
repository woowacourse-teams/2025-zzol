# experiment — 최소 계약 실험 페이로드

[최소 계약 실험](../docs/minimal-contract-experiment.md)의 샌드박스 준비 스크립트가 사용하는 파일들이다. **어떤 Gradle 모듈에도 속하지 않으므로 빌드·spotless·테스트 대상이 아니다.**

## 구조

```text
experiment/
└── sandbox/
    └── inject/          # 샌드박스 베이스 브랜치에 그대로 덮어쓰는 트리 (경로가 곧 목적지)
        └── game/src/test/java/coffeeshout/cardgame/proof/
            └── CardGameRuleProofTest.java   # 규칙 증명 스켈레톤 — 후보 구현자가 채운다
```

## 규칙 증명 스켈레톤

`CardGameRuleProofTest`는 메서드명만 있고 본문은 전부 `fail("TODO ...")`다. 후보 구현자가 자기 아키텍처로 8건을 전부 채워야 Tier 0 게이트를 통과한다. 메서드 삭제·개명·시그니처 변경은 채점 스크립트가 주입 원본과의 diff로 잡는다. 판정 규약은 [규칙 명세서](../docs/minimal-contract-rules.md)의 실험 수칙 절을 따른다.

이 파일을 모듈 소스에 두지 않는 이유: 저장소에 JUnit 태그 필터가 없어 `fail("TODO")`가 든 채로 모듈에 두면 일반 CI가 실패한다.
