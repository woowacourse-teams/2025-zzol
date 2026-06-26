---
description: 변경된 파일 전체를 대상으로 zzol 스타일 규칙 위반을 일괄 감사한다. style-check(자동 단일 파일)와 달리 프로젝트 범위 스캔.
argument-hint: '[파일경로 ...] [--fix]'
allowed-tools: Read, Glob, Grep, Bash
---

# style-check

zzol 프로젝트의 스타일 일관성을 검사한다.
`--fix` 인자가 있으면 자동 수정 가능한 항목을 직접 수정한다.

## 1. 컨텍스트 로드

아래 파일을 읽어 사용 가능한 토큰 목록을 파악한다.

- `src/styles/theme.ts` — `theme.color.*`, `theme.typography.*`
- `src/constants/zIndex.ts` — `Z_INDEX.*`
- `src/constants/design.ts` — `DESIGN_TOKENS.*`
- `.claude/rules/style.md` — 전체 스타일 규칙

## 2. 검사 대상 파일 확정

`$ARGUMENTS`에 파일 경로가 명시되면 해당 파일만 검사한다.
명시되지 않은 경우 `git diff --name-only HEAD~1` 에서 `src/` 경로이고 `.ts` / `.tsx`인 파일을 추출한다.

staged 변경만 확인하려면 `git diff --cached --name-only`를 사용한다.

## 3. 검사 항목

### 3-1. 색상 하드코딩

다음 패턴을 Grep으로 탐지한다:

```text
# hex 리터럴
'#[0-9a-fA-F]{3,8}'

# CSS 색상 키워드 (theme 토큰 외부에서)
: (white|black|red|blue|yellow|gray|grey)[;,\s]

# rgba/rgb (모달 백드롭 rgba(0,0,0,N) 제외)
rgba\([1-9]|rgb\(
```

`theme.color.white`, `theme.color.black` 등 토큰 참조는 제외한다.

### 3-2. 타이포그래피 하드코딩

```text
font-size: \d+(px|rem|em)
font-weight: \d{3}
line-height: \d+\.?\d*[^;]    # theme 토큰 외부
```

`theme.typography` 참조는 제외한다.

### 3-3. z-index 하드코딩

```text
z-index: \d+
```

`Z_INDEX\.` 참조는 제외한다.

### 3-4. 인라인 스타일

```tsx
style=\{\{
```

테스트 파일(`*.test.tsx`, `*.stories.tsx`) 제외.

### 3-5. styled 파일 컨벤션

- `.tsx` 파일이 `import * as S from` 패턴을 따르는지 확인
- `.styled.ts` 파일에서 `Props` 타입의 prop에 `$` 접두사가 없는 경우 탐지

### 3-6. 임포트 패턴

- `styled` 파일에서 `import styled from '@emotion/styled'` 없이 스타일 정의 시 오류

## 4. 출력 형식

```text
## 스타일 검사 결과

### 요약
- 검사 파일: N개
- 위반 항목: N개
- 자동 수정 가능: N개

---

### [파일경로]

#### 색상 하드코딩
- L42: `background: '#F53E41'` → `background: theme.color.point[400]`

#### 타이포그래피 하드코딩
- L49: `font-size: 15px` → `${({ theme }) => theme.typography.paragraph.fontSize}`

#### z-index 하드코딩
- L7: `z-index: 999` → `z-index: ${Z_INDEX.MODAL}`

#### 인라인 스타일
- L18: `style={{ color: '#333' }}` → `.styled.ts`로 이동 + `theme.color.gray[700]`

---

### 이슈 없음
- [파일경로]
```

## 5. --fix 모드

`$ARGUMENTS`에 `--fix`가 포함된 경우:

1. **자동 수정 가능** 항목 (단순 치환):
   - z-index 숫자 → `Z_INDEX.*` 상수 (정확히 매핑되는 값만)
   - `theme.color.white` / `theme.color.black` 으로 치환 가능한 `'white'` / `'black'` / `'#FFFFFF'` / `'#000000'`

2. **수동 수정 필요** 항목은 수정하지 않고 보고만 한다.
   - 임의 hex 색상 (어떤 토큰인지 판단 불가)
   - 인라인 스타일 (구조 변경 필요)
   - 타이포그래피 (의도한 스타일 수준 판단 불가)

수정 후 변경 파일 목록과 수정 건수를 출력한다.

## 절대 규칙

- `--fix` 없이는 `src/` 파일을 **절대 수정하지 않는다**
- 수정 제안은 출력으로만 전달한다
