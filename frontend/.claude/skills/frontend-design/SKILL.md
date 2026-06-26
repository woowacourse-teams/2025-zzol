---
description: zzol 디자인 시스템 안에서 완성도 높은 프론트엔드 UI를 만든다. 컴포넌트 요청 시 디자인 방향을 먼저 잡고, theme 토큰·Emotion 패턴·스타일 규칙을 지키며 component + .styled.ts + Story까지 한번에 생성한다.
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# frontend-design

zzol 디자인 시스템 안에서 **완성도 있는 UI 컴포넌트**를 만든다.
generic한 결과물 대신, 서비스 맥락에 맞는 의도적인 디자인 방향을 선택하고 실행한다.

## zzol 서비스 맥락

- 미니게임 기반 당첨자 추첨 서비스 — 긴장감, 게임적 쾌감, 브랜드 컬러(포인트 레드)가 핵심 무드
- 모바일 우선(max-width: 430px), 하단 탭바 60px 고려
- 로그인·대기실·게임·결과 등 고-에너지 상태 전환이 많은 서비스

## 실행 순서

1. **컨텍스트 파악**
   - `$ARGUMENTS`에서 컴포넌트 목적, 위치(계층), 사용처를 확인
   - 불명확하면 멈추고 질문
   - 유사 컴포넌트가 있는지 Grep으로 확인 후 재사용 가능 여부 판단

2. **디자인 방향 결정**
   - 아래 방향 중 하나를 선택하고 이유를 한 줄로 명시
     - **임팩트형**: 포인트 컬러 강조, 굵은 타이포, 큰 여백 — CTA·결과 화면
     - **긴장감형**: 어두운 배경, 레드-화이트 대비, 미묘한 애니메이션 — 게임 진행 중
     - **클린형**: 회색 계열, 작은 타이포, 최소 장식 — 설정·목록·폼
     - **축제형**: 포인트 컬러 + yellow 액센트, 스태거 애니메이션 — 당첨·완료 상태
   - generic한 blue/gray 기본값, 복사-붙여넣기 스타일은 금지

3. **구현 파일 결정**
   - `src/components/@common/` 또는 `@composition/` → component.tsx + component.styled.ts + component.stories.tsx
   - `src/features/<domain>/components/` → component.tsx + component.styled.ts (Story 선택)
   - `src/features/<domain>/pages/` → component.tsx + component.styled.ts

4. **theme 토큰 및 스타일 규칙 적용**
   - 구현 전 `.claude/rules/style.md`와 `src/styles/theme.ts`를 읽어 토큰을 확인
   - hex 리터럴 금지 → `theme.color.*`
   - font-size/weight 하드코딩 금지 → `theme.typography.*`
   - z-index 숫자 금지 → `Z_INDEX.*`
   - 반응형 고정 치수 → `DESIGN_TOKENS.*`
   - HTML에 전달 불필요한 prop → `$` transient prefix

5. **인터랙션 & 애니메이션**
   - 버튼 hover/press → `buttonHoverPress` helper
   - 입장/퇴장 애니메이션 → `keyframes` from `@emotion/react`
   - 단순 피드백 → `&:active { opacity: 0.85; }`
   - 애니메이션은 **있으면 더 좋은 것**이 아닌 **목적이 있을 때만** 추가

6. **Story 작성 (해당하는 경우)**
   - `@storybook/test` 미설치 — `fn()` import 금지, 핸들러는 `() => {}` 사용
   - 주요 variant를 모두 커버하는 Story 작성
   - `storybook-conventions` 스킬 규칙 적용

7. **style-check 실행**
   - 작성한 `.styled.ts` 파일에 대해 `style-check` 스킬 체크리스트를 적용
   - 위반 항목 즉시 수정

## 출력 형식

```text
## 디자인 방향
[선택한 방향]: [이유 한 줄]

## 생성 파일
- `경로/ComponentName.tsx`
- `경로/ComponentName.styled.ts`
- `경로/ComponentName.stories.tsx` (해당 시)

[코드 블록]
```

## 절대 규칙

- 기존 파일을 요청 없이 수정하지 않는다
- generic AI 패턴(blue primary, gray secondary, rounded-full 버튼) 지양
- 디자인 방향 없이 바로 코드 작성 금지
