---
globs:
  - "src/features/**"
---

## Feature 슬라이스 구조

> `src/features/miniGame/**`은 `minigame-structure` 스킬을 우선 적용한다.

### 디렉터리 배치

```
src/features/<featureName>/
├── components/   feature 전용 UI — api 직접 호출 금지
├── hooks/        서버 상태·로직 — useFetch/useMutation 사용 (api-conventions 참조)
├── pages/        라우트 1:1 컴포넌트 — 비즈니스 로직 없음
├── services/     외부 SDK·복잡한 추상화만 (단순 REST는 hooks/로)
└── types.ts      feature 전용 타입 (선택)
```

### 네이밍

| 항목 | 형식 | 예 |
|---|---|---|
| 디렉터리 | camelCase | `auth`, `entry`, `home` |
| 페이지 | PascalCase + `Page` | `EntryNamePage.tsx` |
| 훅 | `use` + PascalCase | `useMyStats.ts` |

### Service 패턴 (필요 시)

인터페이스와 구현체를 분리하고 `index.ts`에서 DI한다.

```ts
// FooService.ts — 인터페이스
export interface FooService { doSomething(): Promise<void>; }

// BackendFooService.ts — 구현체
export class BackendFooService implements FooService { ... }

// index.ts — 엔트리
export const fooService: FooService = new BackendFooService();
```

### 체크리스트

- [ ] 라우트 추가 시 `src/router.tsx` 등록 확인
- [ ] WebSocket 구독이 필요하면 Context Provider 생성 (`websocket.md` 참조)
- [ ] `@common`·`@composition` 레벨로 컴포넌트를 올릴 때 Storybook 필수
