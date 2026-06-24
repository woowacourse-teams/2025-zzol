## zzol 스타일링 규칙

### 금지 항목 요약

| 금지 | 대체 |
|---|---|
| hex 리터럴 (`'#888'`, `'#fff'`) | `theme.color.*` |
| `color: 'white'`, `background: 'black'` | `theme.color.white`, `theme.color.black` |
| `font-size: 15px` | `theme.typography.*` |
| `z-index: 999` | `Z_INDEX.*` |
| `style={{ color: '#333' }}` JSX 인라인 스타일 | `.styled.ts` + `theme.color.*` |
| `width: 54px` (반응형 UI 요소) | `DESIGN_TOKENS.*` |

---

### 파일 구조 & 임포트

스타일은 반드시 `ComponentName.styled.ts`로 분리한다. 컴포넌트 내부 인라인 `style={{}}` 금지.

```ts
// styled 파일 임포트 순서
import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';       // 필요할 때만
import { Z_INDEX } from '@/constants/zIndex';          // z-index 사용 시
import { DESIGN_TOKENS } from '@/constants/design';    // 반응형 카드/원 크기 사용 시
```

컴포넌트에서 네임스페이스 임포트:

```ts
import * as S from './ComponentName.styled';
// <S.Container>, <S.Title> ...
```

---

### 색상 토큰 — `theme.color.*`

hex 리터럴(`'#888'`, `'#fff'`)은 **위치 무관하게 금지**. JSX prop(`fill`, `stroke`, `color`, `backgroundColor`)도 포함.

| 그룹 | 키 | 용도 |
|---|---|---|
| `theme.color.point.{50~500}` | 브랜드 컬러 (레드 계열) | 주요 CTA, 강조 |
| `theme.color.gray.{50~950}` | 그레이 스케일 | 배경, 텍스트, 구분선 |
| `theme.color.white` / `theme.color.black` | 단색 | |
| `theme.color.yellow` | 옐로우 | |
| `theme.color.oauth.{google,kakao,naver}` | OAuth 버튼 전용 | 로그인 버튼만 사용 |

```ts
// ✅
background: ${({ theme }) => theme.color.point[400]};
color: ${({ theme }) => theme.color.white};

// ❌
background: '#F53E41';
color: 'white';
```

**반투명 처리**: rgba 대신 hex에 opacity 2자리를 붙인다.

```ts
// ✅
box-shadow: 0 4px 20px ${({ theme }) => theme.color.point[400]}59;  // 35% opacity

// ❌
box-shadow: 0 4px 20px rgba(245, 62, 65, 0.35);
```

예외적으로 `rgba(0, 0, 0, N)` 형태의 모달 백드롭/그림자는 허용한다.

---

### 타이포그래피 — `theme.typography.*`

`font-size`, `font-weight`, `line-height`를 개별로 하드코딩하지 않는다.
`theme.typography.*`를 스프레드하거나 각 속성을 토큰으로 참조한다.

| 토큰 | `fontSize` | `fontWeight` | 용도 |
|---|---|---|---|
| `h1` | clamp(24→30px) | 700 | 메인 타이틀 |
| `h2` | clamp(20→24px) | 600 | 섹션 제목 |
| `h3` | clamp(18→20px) | 600 | 서브 제목 |
| `h4` | clamp(14→16px) | 600 | 소제목, 버튼 텍스트 |
| `paragraph` | clamp(14→16px) | 500 | 본문 |
| `small` | clamp(12→14px) | 400 | 보조 텍스트 |
| `caption` | clamp(11→12px) | 400 | 캡션 |

```ts
// ✅ — 스타일 블록 전체 스프레드 (권장)
${({ theme }) => theme.typography.h4}

// ✅ — 개별 속성 참조
font-size: ${({ theme }) => theme.typography.paragraph.fontSize};
font-weight: ${({ theme }) => theme.typography.paragraph.fontWeight};
line-height: ${({ theme }) => theme.typography.paragraph.lineHeight};

// ❌
font-size: 15px;
font-weight: 700;
```

---

### Z-index — `Z_INDEX.*`

임의 숫자 금지. 반드시 `@/constants/zIndex`의 상수를 사용한다.

```ts
import { Z_INDEX } from '@/constants/zIndex';

// ✅
z-index: ${Z_INDEX.MODAL};
z-index: ${Z_INDEX.TOAST};

// ❌
z-index: 999;
z-index: 1000;
```

| 상수 | 값 | 용도 |
|---|---|---|
| `TOGGLE_BUTTON_THUMB` | 0 | 토글 썸 |
| `TOGGLE_BUTTON_OPTION` | 1 | 토글 옵션 |
| `ROULETTE_PIN` | 10 | 룰렛 핀 |
| `BACKDROP` | 990 | 백드롭 |
| `MODAL` | 999 | 모달 |
| `TOAST` | 1000 | 토스트/배너 |

---

### 반응형 크기 — `DESIGN_TOKENS.*`

카드·원 치수처럼 반응형이 필요한 고정 UI 요소에 사용한다. 타이포그래피는 `theme.typography`로 접근한다(`DESIGN_TOKENS.typography`는 내부 구현).

```ts
import { DESIGN_TOKENS } from '@/constants/design';

// ✅
width: ${DESIGN_TOKENS.card.medium.width};
height: ${DESIGN_TOKENS.card.medium.height};

// ❌
width: 54px;
height: 60px;
```

---

### Emotion 작성 패턴

#### Transient props

HTML 요소로 전달되지 않아야 하는 prop은 `$` 접두사를 붙인다.

```ts
type Props = {
  $variant: 'primary' | 'secondary';
  $isLoading: boolean;
  $touchState: TouchState;
};

export const Container = styled.button<Props>`
  background: ${({ $variant, theme }) =>
    $variant === 'primary' ? theme.color.point[400] : theme.color.gray[50]};
`;
```

#### css helper — 조건부/복합 스타일

```ts
import { css } from '@emotion/react';

${({ $variant, theme }) =>
  $variant === 'disabled' && css`
    background: ${theme.color.gray[200]};
    color: ${theme.color.white};
    cursor: default;
    pointer-events: none;
  `}
```

#### keyframes — 애니메이션

```ts
import { keyframes } from '@emotion/react';

const slideInUp = keyframes`
  from { transform: translateX(-50%) translateY(100%); opacity: 0; }
  to   { transform: translateX(-50%) translateY(0);    opacity: 1; }
`;

export const Container = styled.div`
  animation: ${slideInUp} 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
`;
```

---

### 인터랙션 패턴

버튼 hover/press 효과는 `buttonHoverPress` 헬퍼를 사용한다.

```ts
import { buttonHoverPress } from '@/styles/animations/buttonHoverPress';

${({ theme, $touchState }) =>
  buttonHoverPress({ activeColor: theme.color.point[500], touchState: $touchState })}
```

단순 active 피드백만 필요할 때:

```ts
&:active {
  opacity: 0.85;
}
```

---

### 고정 레이아웃 패턴

토스트/배너 공통 위치:

```ts
position: fixed;
bottom: 82px;          // 하단 탭바(60px) + 여백(22px)
left: 50%;
transform: translateX(-50%);
width: 90%;
max-width: 400px;
z-index: ${Z_INDEX.TOAST};
```

모바일 뷰포트 기준 최대 너비:

```ts
max-width: 430px;
margin: 0 auto;
```

