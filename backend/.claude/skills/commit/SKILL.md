---
name: commit
description: 변경된 파일을 기능 단위로 그룹화하고, 각 그룹의 관련 테스트를 실행한 후 순서대로 커밋한다.
argument-hint: "[커밋 메시지 (선택)] [--all: 그룹 분리 없이 전체 커밋]"
allowed-tools: Bash, Read, Glob, Grep, Skill
---

# commit

변경된 파일을 기능 단위로 묶어 순서대로 커밋한다. 각 커밋 전 관련 테스트를 실행해 통과를 확인한다.

## Step 1: 변경 파일 파악

```bash
git status --short
```

스테이지된 파일과 수정된 파일(untracked 포함)을 모두 수집한다.
변경이 없으면 "커밋할 변경사항이 없습니다"를 출력하고 종료한다.

## Step 2: 기능 단위 그룹화

`--all` 플래그가 있으면 Step 2를 건너뛰고 전체를 하나의 그룹으로 처리한다.

변경 파일을 다음 기준(우선순위 순)으로 그룹화한다:

1. **도메인 패키지** — `coffeeshout.<domain>.*` 하위 파일끼리 묶는다
2. **계층 + 도메인** — 같은 도메인에 여러 계층이 섞여 있으면 더 세분화한다
3. **테스트 파일** — 대응 프로덕션 코드와 같은 그룹에 포함한다
   - `src/test/java/coffeeshout/A/FooTest.java` → `src/main/java/coffeeshout/A/Foo.java`와 같은 그룹
   - 대응 프로덕션 변경이 없는 테스트 파일만 있으면 별도 그룹으로 분리한다
4. **설정·빌드 파일** — `build.gradle`, `application*.yml`, `docker-compose*.yml` 등은 별도 그룹

그룹화 결과를 다음 형식으로 사용자에게 제시한다:

```text
[그룹 1] feat(catalog): WsCatalog 캐싱 개선
  M src/main/java/coffeeshout/websocket/catalog/WsCatalog.java
  M src/main/java/coffeeshout/websocket/catalog/WsCatalogBuilder.java
  M src/test/java/coffeeshout/websocket/catalog/WsCatalogBuilderTest.java

[그룹 2] test(docs): catalog fixture 생성기 정리
  M src/test/java/coffeeshout/websocket/docs/WsCatalogFixtureGeneratorTest.java

변경·병합·재정렬하려면 알려주세요. 그대로 진행할까요?
```

사용자 확인 후 Step 3으로 넘어간다.

## Step 3: 관련 테스트 병렬 실행

커밋은 그룹 순서대로 하지만, **테스트는 커밋 전에 모든 그룹을 한 번에 병렬로 돌린다.** 그룹마다 하나씩 순차로 기다리지 않는다.

### 3-1. 관련 테스트 식별

그룹에 **프로덕션 파일**이 포함된 경우에만 테스트를 실행한다.

- 파일 경로에서 공통 패키지를 추출한다
  - `src/main/java/coffeeshout/websocket/catalog/WsCatalog.java` → `coffeeshout.websocket.catalog`
- 같은 패키지 하위를 모두 테스트한다: `coffeeshout.websocket.catalog.*`
- 한 그룹이 여러 패키지/모듈에 걸치면 해당 테스트 클래스를 모두 대상에 포함한다

테스트 파일만 변경된 그룹은 테스트 실행 대상에서 제외한다.

### 3-2. 병렬 실행

프로덕션 파일이 포함된 모든 그룹의 테스트를 **한 메시지에서 동시에** 띄운다. `/run-tests ... --sync`는 내부적으로 Agent 1회 호출이므로, 그룹 수만큼의 Agent를 같은 응답에서 병렬로 발행해 한꺼번에 결과를 받는다. 그룹 하나씩 순차로 기다리지 않는다.

결과 처리:

- **전부 통과** → Step 4(순차 커밋)로 넘어간다
- **일부 실패** → 실패한 그룹과 실패 원인을 사용자에게 보고하고 처리 방법(수정 후 재시도 / 이 그룹 건너뜀 / 전체 중단)을 선택하게 한다
  - **수정 후 재시도**: 수정한 코드는 뒤 순서 그룹에도 영향을 줄 수 있으므로, **수정한 그룹과 그 이후 순서의 모든 그룹 테스트를 다시 병렬로 실행**한다. 이미 통과했고 수정에 영향받지 않는 앞 순서 그룹은 재실행하지 않는다
  - **건너뜀**: 해당 그룹을 커밋 대상에서 제외하고 나머지 통과 그룹만 진행한다

## Step 4: 순차 커밋

모든 관련 테스트가 통과한 뒤, 그룹 순서대로 스테이징·커밋한다.

```bash
git add <그룹 내 파일 목록>
```

커밋 메시지 결정:
- `$ARGUMENTS`에 메시지가 있으면 그룹 scope를 붙여 사용
  - 예: `fix: 캐시 초기화 누락` + 그룹이 `catalog` → `fix(catalog): 캐시 초기화 누락`
  - 그룹이 하나뿐이면 scope 없이 그대로 사용
- `$ARGUMENTS`가 없으면 변경 내용을 분석해 자동 생성
  - 형식: `type(scope): 한국어 설명` (70자 이내)
  - type 선택 기준: 새 기능=`feat`, 버그=`fix`, 리팩토링=`refactor`, 테스트만=`test`, 설정=`chore`, 문서=`docs`

```bash
git commit -m "$(cat <<'EOF'
<커밋 메시지>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

커밋 후 다음 그룹으로 넘어간다.

## Step 5: 완료 보고

모든 그룹 커밋 후 생성된 커밋을 보여준다:

```bash
git log --oneline -<커밋한 수>
```
