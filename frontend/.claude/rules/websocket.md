---
globs:
  - "src/apis/websocket/**"
  - "src/contexts/**"
---

## WebSocket 컨벤션

### 핵심 원칙

- 구독은 반드시 `useWebSocketSubscription` 사용 — `useWebSocket().subscribe` 직접 호출 금지
- 발행은 `useWebSocket().send` 사용
- 구독은 **Provider** 또는 **훅**에서만 — 컴포넌트에서 직접 구독 금지
- destination prefix(`/topic`, `/app`)는 내부 자동 추가됨 — 경로에 포함하지 않는다

### destination 형식

```ts
// ✅
useWebSocketSubscription(`/room/${joinCode}/gameState`, handler);
send(`/room/${joinCode}/action`, payload);

// ❌ prefix 중복
useWebSocketSubscription(`/topic/room/${joinCode}/gameState`, handler);
```

### useWebSocketSubscription 시그니처

```ts
useWebSocketSubscription(
  destination: string,
  onData: (data: T) => void,
  onError?: (error: Error) => void,
  enabled?: boolean   // 기본값 true — 조건부 구독에 useEffect 분기 대신 사용
)
```

### Provider 구독 패턴

```tsx
const FooProvider = ({ children }: PropsWithChildren) => {
  const { joinCode } = useIdentifier();

  useWebSocketSubscription(`/room/${joinCode}/fooState`, handleFooState);

  return <FooContext.Provider value={...}>{children}</FooContext.Provider>;
};
```

### send 패턴

```ts
const { send } = useWebSocket();

send(`/room/${joinCode}/action`, { type: 'SELECT', value: id });
send(`/room/${joinCode}/ready`);   // body 없는 경우
```

### onData 안정화

`onData` 콜백은 `useCallback` 또는 `useRef`로 안정화해 불필요한 재구독을 막는다.
