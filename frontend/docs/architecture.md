# 아키텍처

## 라우팅

`src/router.tsx`는 React Router를 쓴다. `HomePage`·`EntryNamePage`만 정적 import고 나머지 페이지는 lazy-load다. lazy 청크를 못 받아 `ChunkLoadError`가 나면 새 `index.html`을 받도록 한 번 새로고침한다(#1857).

- `/` — 홈페이지
- `/entry/name` — 닉네임 입력 페이지
- `/room/:joinCode` — RoomLayout. WebSocket 컨텍스트 경계다
  - `/lobby` — 대기실
  - `/roulette/play`, `/roulette/result` — 룰렛 게임
  - `/:miniGameType/ready|play|result` — 미니게임 페이지. `MiniGameProviders`로 래핑한다
- `/join/:joinCode` — QR 딥링크 입장 페이지
- `/auth/callback`, `/auth/terms` — OAuth 콜백, 약관 동의
- `/privacy` — 개인정보 처리방침
- `/guide`, `/games`, `/games/:slug` — SEO 콘텐츠 페이지. `SeoContentPage` 하나가 셋을 담당하며 본문은 `src/seo/pages.json`에서 읽는다
- `*` — NotFoundPage

## Provider 계층

`src/App.tsx`에서 아래 순서로 중첩:

1. ThemeProvider — Emotion
2. InstallPromptProvider — PWA 설치 프롬프트. devtools가 켜졌을 때의 `DevToolsWrapper`와 `UpdateBanner`가 이 안에 있다
3. AuthProvider — 로그인 세션
4. UserSocketProvider — 개인 소켓
5. IdentifierProvider — 참가 코드와 닉네임. sessionStorage에 영속한다
6. ParticipantsProvider
7. WebSocketProvider — 방 소켓, STOMP over SockJS
8. PlayerTypeProvider
9. ProbabilityHistoryProvider
10. GlobalErrorBoundary
11. ToastProvider
12. ModalProvider
13. FriendsProvider. 라우트 `Outlet`과 `ModalOutlet`이 이 안에 있어 모달 내용도 친구 컨텍스트를 읽는다

CardGame, RacingGame 같은 미니게임별 컨텍스트는 미니게임 라우트 내에서만 `MiniGameProviders`가 추가한다.

## 상태 관리

외부 상태 라이브러리 없이 React Context API만 사용:

- **IdentifierProvider** — 참가 코드, 닉네임, QR URL. sessionStorage에 둔다
- **ParticipantsProvider** — 참가자 목록
- **WebSocketProvider** — STOMP 연결, 자동 재연결, 메시지 복구

## WebSocket 레이어

`src/apis/websocket/`은 `@stomp/stompjs` + `sockjs-client` 기반 STOMP 프로토콜:

- `useWebSocketConnection` — 연결 라이프사이클
- `useWebSocketMessaging` — publish/subscribe
- `useWebSocketReconnection` — 구독 레지스트리 기반 자동 복구
- `useStompSessionWatcher` — 세션 상태 추적

### 계약 타입

destination 과 payload 타입은 손으로 쓰지 않고 BE 가 `src/apis/websocket/generated/`에 생성한다. 세 파일이 있고 모두 커밋한다.

| 파일 | 만드는 쪽 | 담는 것 |
| --- | --- | --- |
| `wsContract.ts` | BE `WsCatalogContractTest` | destination union `WsSubscribePath`·`WsSendPath`, destination 에서 payload 를 찾는 `WsPayloadOf<D>`, 동치 검사 `WsSubscribeDestination<D>`·`WsSendDestination<D>`, payload 이름 alias |
| `ws-openapi.json` | BE `WsCatalogContractTest` | payload record 의 OpenAPI 스키마. `@Nullable` 필드만 `required` 에서 빠지고 `nullable` 이다 |
| `wsOpenApi.d.ts` | openapi-typescript를 쓰는 `npm run generate:ws` | 위 JSON 에서 만든 TS 타입. `wsContract.ts` 가 이름을 다시 내보내므로 직접 import 하지 않는다 |

`useWebSocketSubscription(destination, onData)` 의 파라미터 타입이 이 파일을 받는다. 카탈로그에 없는 경로는 `` `ws 카탈로그에 없는 destination: …` `` 오류로, 어긋난 payload 필드는 그 필드를 쓰는 줄의 오류로 나온다. `src/types/**` 에서 WS payload 타입은 생성 타입의 alias 다. 클라이언트 전용 타입은 거기에 손으로 쓴다.

BE 계약이 바뀌면 BE PR 이 세 파일을 함께 갱신해 온다. 낡은 생성물은 CI 가 막는다. backend-ci 는 `wsContract.ts`·`ws-openapi.json`을, frontend-ci 는 `wsOpenApi.d.ts`를 본다. 로컬에서 BE 를 고쳤을 때는 아래 두 명령으로 갱신하고, pre-push 훅이 같은 일을 이어 돌린다.

```bash
backend/gradlew -p backend :app:test --tests '*WsCatalogContractTest*'
npm run generate:ws
```

결정 배경은 [ADR](adr/20260915-ws-contract-generated-types.md)에 있다.

## REST API 레이어

`src/apis/rest/`에 fetch를 래핑한 커스텀 훅이 있다:

- `useFetch` — 즉시 GET
- `useLazyFetch` — 지연 GET
- `useMutation` — POST/PUT/PATCH/DELETE. 에러가 나면 Toast로 알린다

## 컴포넌트 계층

- `src/components/@common/` — Button, Modal, Toast 같은 디자인 시스템 원자 컴포넌트
- `src/components/@composition/` — PlayerCard, ProbabilityList 같은 중간 조합 컴포넌트
- `src/features/` — 기능별 페이지 및 로직
- `src/layouts/` — 공통 레이아웃 껍데기

스타일링은 **Emotion** CSS-in-JS. 스타일 파일은 `.styled.ts` 컨벤션. 디자인 토큰은 `src/styles/theme.ts`.

## 미니게임 패턴

각 미니게임은 `src/features/miniGame/<gameName>/` 하위에 Ready·Play 페이지를 담는 `pages/`와 `components/`를 두고, 필요하면 `hooks/`·`constants/`를 더한다. 게임별 상태 Provider는 feature 밖 `src/contexts/<GameName>/`에 있다.

게임은 `src/features/miniGame/config/gameConfigs.tsx`의 `GAME_CONFIGS`에 Provider·페이지·설명 슬라이드를 등록한다. `MiniGameProviders`가 `miniGameType` 파라미터를 보고 그 Provider를 주입한다. 게임 종류의 SSOT는 `src/types/miniGame/common.ts`의 `MINI_GAME_NAME_MAP`이다.

## 인증 세션 복원

앱 로드 시 `AuthProvider`의 `bootstrap` 함수가 localStorage의 액세스 토큰 유무를 확인하고, 토큰이 있을 때만 `GET /users/me`로 세션을 복원한다. 토큰이 없으면 API 호출 없이 익명 상태로 시작한다. 토큰 저장소는 `LocalStorageTokenStore`다.

## DevTools

`process.env.ENABLE_DEVTOOLS`가 truthy일 때만 iframe 자동 테스트 패널과 네트워크 디버거를 담은 `DevToolsWrapper`가 렌더링된다. 로컬은 `.env.development`의 `ENABLE_DEVTOOLS=true`로 켠다. 배포 빌드는 CI가 `.env.production`에 쓰는 값으로 갈린다. `dev` 브랜치는 `true`, `prod`는 `false`다.

**기능별 가드도 `ENABLE_DEVTOOLS` 기준을 따른다.** `useMockMode`와 `useServiceWorkerUpdate`는 `NODE_ENV`가 아니라 `Boolean(process.env.ENABLE_DEVTOOLS)`를 본다. dev·prod 배포 빌드가 둘 다 production 모드라 `NODE_ENV`로는 두 환경을 가를 수 없다. 패널 노출과 기능 동작이 같은 변수로 묶인다.

## 빌드

Vite가 아닌 **Webpack** 사용. 설정은 `webpack.common.js`, `webpack.dev.js`, `webpack.prod.js`로 분리. `@/*` → `src/*` path alias는 TypeScript와 Webpack 양쪽에 모두 설정됨. 환경변수는 빌드 타임에 `.env.${mode}`에서 읽는다. dev 서버는 `.env.development`, `npm run build`는 `.env.production`이다.

Sentry는 `src/main.tsx`에서 프로덕션 전용으로 초기화되며, `@sentry/webpack-plugin`으로 소스맵 업로드가 통합되어 있다.

## 배포 인프라

**GitHub Actions → S3 Deploy** 구조다. 워크플로는 `.github/workflows/frontend-cd.yml`이다. `dev`나 `prod`에 `frontend/**` 변경이 push되면 lint·test·build를 하는 `frontend-ci.yml`을 재사용해 `frontend/dist`를 아티팩트로 만들고, OIDC로 AWS Role을 assume해 브랜치별 S3 경로에 올린 뒤 그 환경의 CloudFront 캐시를 무효화한다. 두 환경 모두 production 모드인 `npm run build`이고, `API_URL`과 `ENABLE_DEVTOOLS`만 브랜치에 따라 다르게 굽는다. 장기 AWS access key는 사용하지 않는다.

- contenthash가 붙은 번들·이미지는 1년 immutable 캐시로, 그 외 HTML·service-worker.js·manifest 같은 파일은 `no-cache`로 올린다(#1857). 해시 파일을 먼저 올려 새 HTML이 없는 청크를 가리키는 순간을 없앤다.
- `sync --delete`를 쓰지 않는다. 이번 빌드에 없고 7일 넘게 갱신되지 않은 파일만 지워, 배포 직전에 열린 탭이 옛 청크를 받을 수 있게 한다.
- `robots.txt`, `manifest.json` 같은 정적 파일은 `webpack.common.js`의 CopyWebpackPlugin으로 `dist/`에 복사되어 아티팩트에 포함된다.
- `sitemap.xml`과 라우트별 `{path}/index.html`은 복사가 아니라 `webpack.common.js`가 `src/seo/pages.json`에서 **생성**한다 (#1710).
