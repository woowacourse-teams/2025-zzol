---
description: .styled.ts 파일을 작성하거나 수정한 직후 zzol 스타일 규칙 위반 여부를 자동 검증하고 수정한다. hex 리터럴, z-index 숫자, 타이포그래피 하드코딩, 인라인 스타일 감지.
paths:
  - "**/*.styled.ts"
allowed-tools: Read, Edit
---

# style-check

방금 수정하거나 작성한 `.styled.ts` 파일의 스타일 규칙 위반을 검증한다.

## 실행 순서

1. `.claude/rules/style.md`를 읽어 규칙 기준을 확인한다
2. 대상 파일을 읽는다
   - `$ARGUMENTS`에 파일 경로가 있으면 해당 파일
   - 없으면 방금 수정한 `.styled.ts` 파일
3. 아래 체크리스트를 적용한다
4. 위반 항목을 즉시 수정한다

## 체크리스트

### 색상

- [ ] hex 리터럴(`'#xxx'`) → `theme.color.*` 토큰으로 교체
- [ ] `'white'`, `'black'` 문자열 → `theme.color.white`, `theme.color.black`
- [ ] `rgba(r, g, b, a)` (검정 외) → `${theme.color.*}hex-opacity` 형태
- [ ] 예외: `rgba(0, 0, 0, N)` 모달 백드롭/그림자는 유지

### 타이포그래피

- [ ] `font-size: Npx` → `${({ theme }) => theme.typography.*.fontSize}`
- [ ] `font-weight: N` → `${({ theme }) => theme.typography.*.fontWeight}`
- [ ] 스타일 블록 전체 적용 가능하면 `${({ theme }) => theme.typography.*}` 스프레드 권장

### Z-index

- [ ] `z-index: N` (숫자) → `${Z_INDEX.*}` 상수로 교체
- [ ] 파일 상단에 `import { Z_INDEX } from '@/constants/zIndex'` 추가

### Emotion 패턴

- [ ] HTML 전달 불필요한 styled prop에 `$` 접두사 적용
- [ ] 조건부 스타일은 `css` helper 사용 (`import { css } from '@emotion/react'`)
- [ ] 애니메이션은 `keyframes` helper 사용

## 출력

위반이 없으면: `✅ 스타일 규칙 준수`

위반이 있으면 수정 후:

```text
수정 완료: N건
- L12: rgba(245, 62, 65, 0.3) → ${theme.color.point[400]}4D
- L28: z-index: 999 → ${Z_INDEX.MODAL}
```
