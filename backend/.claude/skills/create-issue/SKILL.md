---
name: create-issue
description: GitHub 이슈를 템플릿 기반으로 생성하고, 이슈 번호로 작업 브랜치를 체크아웃한다.
argument-hint: "[type] 이슈 제목 — type: feat | fix | refactor | chore | docs | test"
allowed-tools: Bash
disable-model-invocation: true
---

# create-issue

## 1. 인자 파싱

`$ARGUMENTS` 형식: `[type] 이슈 제목 및 설명`

- type이 명시된 경우: 첫 단어를 type으로, 나머지를 제목/설명으로 사용
- type이 없거나 인식 불가한 경우: 내용을 분석해 자동 결정

| type | 이슈 템플릿 | 라벨 |
|------|-----------|------|
| feat | feature-template | `✨feat`, `BE` |
| fix  | bug_report | `🐞bug`, `BE` |
| refactor | feature-template | `🛠️refactor`, `BE` |
| chore | feature-template | `⚙️chore` |
| docs | feature-template | `📝docs` |
| test | feature-template | `🧪 test` |

## 2. 현재 이슈 템플릿

아래 내용을 참고해 이슈 본문을 작성한다.

### Bug 템플릿
!`cat ../.github/ISSUE_TEMPLATE/bug_report.md`

### Feature 템플릿
!`cat ../.github/ISSUE_TEMPLATE/feature-template.md`

## 3. 사용자 확인 (필수 — 이 단계를 건너뛰지 않는다)

`gh issue create`를 실행하기 전에 **반드시** 사용자에게 아래 두 항목을 질문한다.
`$ARGUMENTS`에서 충분히 추론 가능한 항목이라도 확인 또는 보완을 요청한다.

**질문 형식 (한 번에 같이 묻는다):**

> 이슈를 생성하기 전에 두 가지를 확인할게요.
>
> 1. **왜 지금 이걸 하는가?**
>    (비즈니스 이유, ADR 연관, 다른 기능의 사전 조건, 긴급도 등)
>    _현재 파악한 내용: {$ARGUMENTS에서 추론한 동기 또는 "명확하지 않음"}_
>
> 2. **완료를 어떻게 검증할 수 있나요? (성공 기준)**
>    (테스트 통과, 특정 동작 확인, 수치 목표 등 — 체크리스트 형태로 알려주세요)
>    _현재 파악한 내용: {$ARGUMENTS에서 추론한 성공 기준 또는 "명확하지 않음"}_

사용자 응답이 돌아온 뒤에만 다음 단계로 진행한다.

## 4. 템플릿 채우기

frontmatter(`---`로 감싼 부분)는 제거하고 본문 섹션만 사용한다.

| 섹션 | 작성 기준 |
|------|---------|
| `### 어떤 이슈인가요?` | `$ARGUMENTS`의 설명을 바탕으로 1~3문장 작성 |
| `### 🎯 왜 지금 이걸 하는가` | 3단계에서 사용자가 답한 내용으로 작성 |
| `### ✅ 성공 기준` | 3단계에서 사용자가 답한 내용으로 체크리스트 작성 |
| `### 연관 이슈` | 언급이 없으면 `없음` |
| `### 작업 마감일` / `### PR 마감일` | 언급이 없으면 `미정` |
| `### 🔧 TODO` | 예상 작업 항목을 체크리스트로 작성 |

## 5. 이슈 생성

```bash
gh issue create \
  --title "[type] 제목" \
  --label "라벨1,라벨2" \
  --assignee "$(gh api user --jq '.login')" \
  --body "$(cat <<'EOF'
<채운 템플릿 본문>
EOF
)"
```

생성 후 출력된 URL에서 이슈 번호를 추출한다.

## 6. 브랜치 생성 및 체크아웃

```bash
git checkout be/dev
git pull origin be/dev
git checkout -b be/{type}/{issue-number}-{slug}
```

- `{slug}`: 이슈 제목을 소문자 + 하이픈으로 변환, 최대 40자
- 한국어 단어는 의미를 유지하는 영문으로 변환

## 6. 완료 출력

```
✅ 이슈 생성: https://github.com/.../issues/{N}
🌿 브랜치:   be/{type}/{N}-{slug}
```
