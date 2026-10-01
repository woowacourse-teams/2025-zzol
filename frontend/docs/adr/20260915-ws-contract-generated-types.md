---
date: 2026-09-15
status: accepted
---

# WebSocket 계약은 BE 가 생성한 타입으로 강제한다

결정 본문은 [backend ADR-0037](../../../backend/docs/adr/0037-fe-be-contract-type-generation.md)이다. 여기서는 FE 에 미치는 영향만 적는다.

## 맥락

FE 는 destination 문자열과 payload 타입을 손으로 써 왔다. BE 가 경로나 필드를 바꿔도 FE 는 배포 뒤에야 알았다. `racing-game/start` 로 보내는데 BE 에는 `racing-game/tap` 핸들러뿐이었고, `CardGameState` 에 `FIRST_LOADING` 이 없었으며, `Player.userId` 는 게스트에서 null 인데 non-null 로 선언돼 있었다. STOMP 는 없는 토픽 구독에도 성공하고 publish 는 ack 이 없어 어디에도 걸리지 않는다.

조회형 도구(api-mcp)로 맞추려 했지만 한 번도 불리지 않았다. 누군가 기억해서 불러야 하는 검사는 검사가 아니다.

## 결정

BE 가 `src/apis/websocket/generated/` 에 타입 파일을 생성하고, 훅의 파라미터 타입이 그 파일을 받아 `tsc` 가 계약을 강제한다. payload 는 Java 타입을 직접 TS 로 옮기지 않고 OpenAPI 문서(`ws-openapi.json`)를 거쳐 `openapi-typescript` 가 만든다. REST 계약(#1741)도 같은 도구를 쓴다.

## FE 에서 달라지는 것

- **생성 파일을 손으로 고치지 않는다.** 고쳐도 CI 신선도 검사가 되돌린다.
- **필드가 필요하면 BE PR 을 선행한다.** BE record 에 필드를 더하고 null 을 넘기는 컴포넌트에 `@Nullable` 을 달면 FE 타입이 따라온다. FE 가 먼저 타입을 늘려 두는 일은 없다.
- **payload 타입을 훅에 명시하지 않는다.** `useWebSocketSubscription<T>(…)` 가 아니라 destination 에서 `onData` 타입이 추론된다. 콜백에 타입을 적을 땐 생성 타입이나 `src/types/**` 의 alias 를 쓴다.
- **destination 은 방 코드를 보간한 템플릿 리터럴로 넘긴다.** `/room/ABCD/winner` 처럼 고정한 문자열은 정상 경로여도 동치 검사에 걸린다.
- **optional 필드는 BE 의 `@Nullable` 과 1:1 이다.** `field?: T | null` 이면 실제로 null 이나 누락이 온다는 뜻이다. 소비처에서 non-null 로 가정하지 않는다.
- BE 계약이 바뀐 PR 에서 FE 호출부가 컴파일 오류로 드러난다. 그 PR 에서 함께 고친다.

## 고려한 대안

backend ADR-0037 의 "고려한 대안"을 따른다. FE 관점에서 하나만 덧붙이면, FE 쪽 스크립트가 fixture 를 TS 로 바꾸는 안은 BE 작업자가 FE 명령을 한 번 더 돌려야 해서 destination 파일은 BE 테스트가 바로 쓰고, payload 만 `openapi-typescript` 가 만들되 pre-push 훅과 frontend-ci 가 신선도를 대조한다.

## 관련

- `.claude/skills/ws-contract/` — 구독·발행 코드를 쓸 때의 절차
- `.claude/rules/websocket.md` — 훅 시그니처와 컨벤션
- `docs/architecture.md` "계약 타입" — 파일 구조와 갱신 명령
