---
description: WebSocket 구독·발행 코드 작성 또는 수정 시 BE 가 생성한 wsContract.ts 로 컨트랙트를 먼저 확인한다. destination 과 payload 타입은 생성 파일이 SSOT 이고 tsc 가 강제한다.
paths:
  - "src/apis/websocket/**"
  - "src/contexts/**"
  - "src/features/**/hooks/use*WebSocket*.ts"
  - "src/features/**/hooks/use*Subscription*.ts"
allowed-tools: Read, Grep
---

# WebSocket 컨트랙트 워크플로우

## 컨트랙트는 어디에 있나

`src/apis/websocket/generated/wsContract.ts` 가 SSOT 다. BE 의 `@WsTopic`·`@WsQueue`·`@WsReceive` 애노테이션과 response record 에서 `WsCatalogContractTest` 가 생성한다. 손으로 고치지 않는다. 소스보다 낡으면 backend-ci 가 실패한다(ADR-0037).

파일 안에 넷이 있다.

- `WsSubscribePath`·`WsSendPath` — FE 훅이 받는 형태의 destination union. topic 은 `/topic` 을, send 는 `/app` 을 뗀 경로이고 개인 큐는 `/user/queue/...` 그대로다.
- payload 타입 — BE record 와 enum 을 그대로 옮긴 것. `@Nullable` 이 붙은 필드만 `field?: T | null` 이다.
- `WsPayloadOf<D>` — destination 에서 payload 타입을 찾는다. 훅의 `onData` 파라미터 타입이 여기서 나온다.
- `WsSubscribeDestination<D>`·`WsSendDestination<D>` — 호출부 리터럴이 카탈로그 패턴 하나와 정확히 같을 때만 통과시키는 검사.

원본 애노테이션이나 record 를 보고 싶으면 그쪽이 더 정확하다.

```bash
grep -rn '@WsTopic(path = "/room' ../backend/
grep -rn 'record LadderStateResponse' ../backend/
```

## 새 구독·발행을 쓸 때

1. 생성 파일에서 destination 을 찾는다. 없으면 BE 에 그 토픽이나 큐가 아직 없다는 뜻이다. 임의로 만들지 않는다. 만들어도 컴파일 오류다.

   ```bash
   grep -n "ladder" src/apis/websocket/generated/wsContract.ts
   ```

2. destination 은 방 코드를 변수로 보간한 템플릿 리터럴로 넘긴다. `` `/room/${joinCode}/ladder/state` `` 처럼. `/room/ABCD/...` 처럼 고정한 문자열은 정상 경로여도 동치 검사에 걸린다.
3. payload 타입은 `useWebSocketSubscription<T>(…)` 로 명시하지 않는다. `onData` 의 파라미터 타입이 destination 에서 추론된다. 콜백에 타입을 적고 싶으면 생성 타입이나 그 alias(`@/types/**`)를 쓴다.
4. 개인 소켓(`useUserSocketSubscription`)은 envelope 를 벗기지 않는다. `onData` 가 `WebSocketSuccess<Payload>` 를 받으므로 `event.data.…` 로 읽는다. 방 소켓은 wrapper 가 envelope 를 풀어 `data` 만 넘긴다.

## 카탈로그에 없는 경로가 필요할 때

BE PR 을 선행한다. 해당 Publisher 나 컨트롤러에 `@WsTopic(path = "...", payload = Xxx.class)` 를 달고 `backend/gradlew -p backend :app:test --tests '*WsCatalogContractTest*'` 로 생성 파일을 갱신해 함께 커밋한다. null 을 넘기는 record 컴포넌트에는 `@Nullable` 을 단다.

## 검증

```bash
npm run type-check
```

카탈로그에 없는 destination 은 `parameter of type '`ws 카탈로그에 없는 destination: …`'` 오류로, 어긋난 payload 필드는 그 필드를 쓰는 줄의 오류로 나온다.
