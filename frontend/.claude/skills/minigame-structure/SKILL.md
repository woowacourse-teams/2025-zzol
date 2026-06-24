---
description: 미니게임 신규 추가 또는 수정 시 디렉터리 구조, Context 위치, GAME_CONFIGS 등록, MiniGameType 추가 절차를 자동 안내한다.
paths:
  - 'src/features/miniGame/**'
  - 'src/contexts/*Game*/**'
allowed-tools: Read, Bash
---

# 미니게임 구조 컨벤션

## 디렉터리 구조

```
src/features/miniGame/<gameName>/
├── components/          # 게임 전용 UI 컴포넌트
│   └── <ComponentName>/
│       ├── <ComponentName>.tsx
│       ├── <ComponentName>.styled.ts
│       └── <ComponentName>.stories.tsx  # 선택 (공개 컴포넌트이면 필수)
├── hooks/               # 게임 로직 훅
│   └── use<GameName><Feature>.ts
├── pages/               # ReadyPage + PlayPage (라우트 1:1)
│   ├── <GameName>ReadyPage.tsx
│   └── <GameName>PlayPage.tsx
└── constants/           # 게임 전용 상수 (선택)
    └── <gameName>Constants.ts
```

Context는 게임 디렉터리 **외부**에 위치한다:

```
src/contexts/<GameName>/
├── <GameName>Context.ts       # createContext + 타입
└── <GameName>Provider.tsx     # Provider 컴포넌트
```

---

## 신규 게임 추가 체크리스트

### 1. MiniGameType 추가

`src/types/miniGame/common.ts` (또는 MiniGameType이 정의된 파일)에 새 게임 타입을 추가한다.

```bash
# 현재 타입 위치 확인
grep -r "MiniGameType" src/types --include="*.ts" -l
```

### 2. Context 생성

`src/contexts/<GameName>/` 디렉터리를 생성하고 Context + Provider를 작성한다.

```ts
// <GameName>Context.ts
import { createContext, useContext } from 'react';

type <GameName>ContextType = { ... };

export const <GameName>Context = createContext<<GameName>ContextType | null>(null);

export const use<GameName> = () => {
  const ctx = useContext(<GameName>Context);
  if (!ctx) throw new Error('use<GameName> must be used within <GameName>Provider');
  return ctx;
};
```

```tsx
// <GameName>Provider.tsx
import { PropsWithChildren } from 'react';
import { <GameName>Context } from './<GameName>Context';

const <GameName>Provider = ({ children }: PropsWithChildren) => {
  // 게임 상태 및 WebSocket 로직
  return (
    <<GameName>Context.Provider value={...}>
      {children}
    </<GameName>Context.Provider>
  );
};

export default <GameName>Provider;
```

### 3. GAME_CONFIGS 등록

`src/features/miniGame/config/gameConfigs.tsx`에 새 게임을 등록한다.

```tsx
import <GameName>Provider from '@/contexts/<GameName>/<GameName>Provider';
import <GameName>ReadyPage from '../<gameName>/pages/<GameName>ReadyPage';
import <GameName>PlayPage from '../<gameName>/pages/<GameName>PlayPage';

export const GAME_CONFIGS: Record<MiniGameType, GameConfig> = {
  // 기존 게임들...
  <GAME_TYPE_KEY>: {
    Provider: <GameName>Provider,
    ReadyPage: <GameName>ReadyPage,
    PlayPage: <GameName>PlayPage,
    slides: [
      { textLines: ['슬라이드 1'], className: 'slide-first' },
      { textLines: ['슬라이드 2'], className: 'slide-second' },
    ],
  },
};
```

### 4. 라우트 등록 확인

라우팅 설정에서 `MiniGamePlayPage`와 `MiniGameReadyPage`가 새 `miniGameType` 파라미터를 처리하는지 확인한다.

```bash
grep -r "miniGameType\|MiniGamePlayPage\|MiniGameReadyPage" src --include="*.tsx" -l
```

---

## 기존 게임 현황

현재 등록된 게임: `CARD_GAME`, `RACING_GAME`, `SPEED_TOUCH`, `BLIND_TIMER`, `BLOCK_STACKING`, `LADDER_GAME`

Context 위치 패턴: `src/contexts/<PascalCase>Game/<PascalCase>GameProvider.tsx`

---

## 네이밍 컨벤션

| 항목            | 형식                | 예                       |
| --------------- | ------------------- | ------------------------ |
| 디렉터리        | camelCase           | `cardGame`, `racingGame` |
| Context 파일    | PascalCase          | `CardGameContext.ts`     |
| Provider 파일   | PascalCase          | `CardGameProvider.tsx`   |
| 훅 파일         | `use` + PascalCase  | `useCardGameActions.ts`  |
| 페이지 파일     | PascalCase + `Page` | `CardGameReadyPage.tsx`  |
| MiniGameType 키 | UPPER_SNAKE_CASE    | `CARD_GAME`              |
