# commit 그룹화 기준

변경 파일을 기능 단위로 묶는 상세 기준과 예시. SKILL.md Step 2에서 참조한다.

## 우선순위 기준

1. **모듈 + 도메인** — 멀티모듈(ADR-0011)이므로 먼저 모듈로 가른다. 경로는 git 루트 기준 `backend/<module>/src/main/java/coffeeshout/<domain>/...`. 같은 모듈·도메인끼리 묶는다.
2. **계층 + 도메인** — 같은 도메인에 여러 계층(`ui`/`application`/`domain`/`infra`)이 섞여 의미가 다르면 더 세분화한다.
3. **테스트 파일** — 대응 프로덕션과 같은 그룹에 넣는다.
   - `backend/room/src/test/java/coffeeshout/room/FooTest.java` → `backend/room/src/main/java/coffeeshout/room/Foo.java`와 같은 그룹.
   - 대응 프로덕션 변경이 없는 테스트만 있으면 별도 그룹으로 분리한다.
4. **문서·설정·빌드** — `*.md`, `build.gradle*`, `application*.yml`, `docker-compose*.yml`, 루트 `.markdownlint*`·`.gitattributes` 등은 코드와 분리해 별도 그룹으로 묶는다.

## untracked 처리

preflight 의 `UNTRACKED` 섹션 파일은 기본적으로 그룹에 넣지 않는다.

- 새 소스 파일(새 스킬 스크립트·신규 클래스 등)은 사용자 확인 후 해당 그룹에 포함한다.
- 생성물(`reports/`, 빌드 산출물 등)은 절대 포함하지 않는다.

## 제시 형식

그룹화 결과를 다음 형식으로 사용자에게 제시하고 확인받는다. 경로는 git 루트 기준으로 적는다.

```text
[그룹 1] feat(catalog): WsCatalog 캐싱 개선
  M backend/websocket/src/main/java/coffeeshout/websocket/catalog/WsCatalog.java
  M backend/websocket/src/main/java/coffeeshout/websocket/catalog/WsCatalogBuilder.java
  M backend/websocket/src/test/java/coffeeshout/websocket/catalog/WsCatalogBuilderTest.java

[그룹 2] docs(adr): 번호 산정·index 행 형식 보강
  M backend/.claude/skills/adr/SKILL.md

변경·병합·재정렬하려면 알려주세요. 그대로 진행할까요?
```
