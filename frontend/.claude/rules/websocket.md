---
paths:
  - '**/src/apis/websocket/**'
  - '**/src/contexts/**'
---

## WebSocket 컨벤션

### 핵심 원칙

- 구독은 반드시 `useWebSocketSubscription` 을 쓴다. `useWebSocket().subscribe` 직접 호출 금지
- 발행은 `useWebSocket().send` 사용
- 구독은 **Provider** 또는 **훅**에서만 — 컴포넌트에서 직접 구독 금지
- destination prefix(`/topic`, `/app`)는 내부에서 자동으로 붙는다. 경로에 포함하지 않는다
- 단, broker destination(`/user/`, `/queue/`로 시작)은 STOMP broker 가 자체 routing 하므로 `/topic` 이 추가되지 않는다. 개인 큐 구독 시 `/user/queue/...` 를 그대로 전달한다 (예: `/user/queue/errors`)

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
useWebSocketSubscription<D extends WsSubscribePath>(
  destination: WsSubscribeDestination<D>,  // BE 가 생성한 src/apis/websocket/generated/wsContract.ts 의 경로만 받는다
  onData: (data: WsPayloadOf<D>) => void,  // payload 타입은 destination 에서 추론된다
  onError?: (error: Error) => void,
  enabled?: boolean   // 기본값 true — 조건부 구독에 useEffect 분기 대신 사용
)
```

destination 은 `` `/room/${joinCode}/round` `` 처럼 방 코드를 보간한 템플릿 리터럴로 넘긴다. 카탈로그에 없는 경로는 컴파일 오류가 난다. 타입 파라미터는 destination 하나뿐이라 `useWebSocketSubscription<T>(…)` 처럼 payload 타입을 명시하지 않는다. 콜백 파라미터에 타입을 적으려면 생성 타입이나 그 alias(`@/types/**`)를 쓴다.

`src/apis/websocket/generated/` 의 세 파일은 손으로 고치지 않는다. BE 컨트랙트 테스트가 `wsContract.ts`(destination union·`WsPayloadOf`)와 `ws-openapi.json`(payload 스키마)을 쓰고, `npm run generate:ws` 가 그 JSON 에서 `wsOpenApi.d.ts` 를 만든다. payload 이름은 `wsContract.ts` 가 `components['schemas'][…]` 로 다시 내보내므로 import 경로는 `wsContract` 하나면 된다.

payload 타입을 Provider 안에서 `type StateMessage = …` 처럼 손으로 쓰지 않는다. 손으로 쓴 타입은 BE 가 필드를 바꿔도 `tsc` 가 못 잡는다. 사다리 2선(#1822)과 블록쌓기 개편(#1832)이 이렇게 들어와 생성 타입과 충돌했다. 생성 타입에 필드가 없으면 FE 타입을 늘리지 말고 BE record 를 먼저 고친다.

생성 타입에서 `?` 가 붙은 필드는 BE `@Nullable` 이라 상태에 따라 빠진다. `setPoles(msg.poles ?? [])` 처럼 기본값을 준다. 상태별로 필드가 달라지는 판별 union 은 만들지 않는다. `msg.state` 로 분기한 뒤 `??` 로 받는다.

BE 응답 record 에 필드를 더하거나 바꾸는 PR 은 생성물도 같이 커밋한다. 순서는 BE 컨트랙트 테스트(`backend/gradlew -p backend :app:test --tests '*WsCatalogContractTest*'`)로 fixture 와 `ws-openapi.json` 을 쓰고, `npm run generate:ws` 로 `wsOpenApi.d.ts` 를 만든다. pre-push 훅이 같은 검사를 돌리고 CI 는 생성물이 낡았으면 실패한다.

개인 소켓 `useUserSocketSubscription` 은 envelope 를 벗기지 않는다. `onData` 가 `WebSocketSuccess<WsPayloadOf<D>>` 를 받으므로 `event.data.…` 로 읽는다.

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
send(`/room/${joinCode}/ready`); // body 없는 경우
```

### onData 안정화

`onData` 콜백은 `useCallback` 또는 `useRef`로 안정화해 불필요한 재구독을 막는다.
