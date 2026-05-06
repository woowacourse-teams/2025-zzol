---
description: @common/@composition 컴포넌트 추가·수정 시 Storybook Story 파일 작성 컨벤션을 자동 적용한다. Meta 구조, argTypes, variant 커버리지, 금지 패턴 포함.
paths:
  - "src/components/@common/**"
  - "src/components/@composition/**"
allowed-tools: Read, Bash
---

# Storybook 컨벤션

`@common`과 `@composition` 컴포넌트는 반드시 `.stories.tsx` 파일이 함께 있어야 한다.
작업 후 `npm run build-storybook`으로 빌드 통과를 확인한다.

---

## 금지 패턴

```ts
// ❌ @storybook/test 미설치 패키지
import { fn } from '@storybook/test';

// ✅ 클릭 핸들러는 빈 함수
args: { onClick: () => {} }
```

---

## 기본 구조

```tsx
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import <ComponentName> from './<ComponentName>';

const meta: Meta<typeof <ComponentName>> = {
  title: 'Common/<ComponentName>',   // @composition이면 'Composition/<ComponentName>'
  component: <ComponentName>,
  tags: ['autodocs'],
  argTypes: {
    // 컨트롤 가능한 prop 정의
    variant: {
      control: { type: 'select' },
      options: ['primary', 'secondary'],
    },
    size: {
      control: { type: 'select' },
      options: ['small', 'medium', 'large'],
    },
  },
};
export default meta;

type Story = StoryObj<typeof <ComponentName>>;
```

---

## Story 네이밍 — variant 커버리지

컴포넌트의 **모든 주요 variant**를 개별 Story로 커버한다.

```tsx
// variant별 Story
export const Primary: Story = { args: { variant: 'primary', ... } };
export const Secondary: Story = { args: { variant: 'secondary', ... } };
export const Disabled: Story = { args: { variant: 'disabled', ... } };
export const Loading: Story = { args: { variant: 'loading', ... } };
```

상태가 없는 컴포넌트는 `Default` 하나로 충분하다.

```tsx
export const Default: Story = { args: { children: '텍스트' } };
```

---

## render 함수가 필요한 경우

훅이나 외부 상태가 필요한 컴포넌트 (Toast, Modal 등):

```tsx
export const Success: Story = {
  render: () => {
    const { showToast } = useToast();
    return (
      <button onClick={() => showToast({ type: 'success', message: '성공' })}>
        토스트 표시
      </button>
    );
  },
};
```

---

## Portal / 루트 의존 컴포넌트

Toast처럼 `#toast-root` 등 DOM 노드가 필요한 컴포넌트는 `decorators`로 주입한다.

```tsx
const meta: Meta<typeof Toast> = {
  ...
  decorators: [
    (Story) => (
      <>
        <div id="toast-root" />
        <Story />
      </>
    ),
  ],
};
```

---

## 신규 컴포넌트 추가 시 체크리스트

1. `<ComponentName>.stories.tsx` 파일이 같은 디렉터리에 있는가
2. `meta.title`이 `Common/<ComponentName>` 또는 `Composition/<ComponentName>` 형식인가
3. `tags: ['autodocs']`가 있는가
4. 컨트롤 가능한 prop에 `argTypes`가 정의되어 있는가
5. 주요 variant가 모두 Story로 커버되는가
6. `@storybook/test`를 import하지 않는가

```bash
# 빌드 확인 (PR 올리기 전 필수)
npm run build-storybook
```
