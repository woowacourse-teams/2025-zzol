# 아키텍처

## 라우팅 (`src/router.tsx`)

React Router v7, 페이지는 모두 lazy-load. 주요 라우트 구조:

- `/` — 홈페이지
- `/entry/name` — 닉네임 입력 페이지
- `/room/:joinCode` — RoomLayout (WebSocket 컨텍스트 경계)
  - `/lobby` — 대기실
  - `/roulette/play`, `/roulette/result` — 룰렛 게임
  - `/:miniGameType/ready|play|result` — 미니게임 페이지 (MiniGameProviders로 래핑)
- `/join/:joinCode` — QR 딥링크 입장 페이지

## Provider 계층 (`src/App.tsx`)

아래 순서로 중첩:

1. ThemeProvider (Emotion)
2. IdentifierProvider (참가 코드, 닉네임 — sessionStorage 영속)
3. ParticipantsProvider
4. WebSocketProvider (STOMP over SockJS)
5. PlayerTypeProvider
6. ProbabilityHistoryProvider
7. GlobalErrorBoundary
8. ToastProvider / ModalProvider

미니게임별 컨텍스트(CardGame, RacingGame 등)는 미니게임 라우트 내에서만 `MiniGameProviders`가 추가한다.

## 상태 관리

외부 상태 라이브러리 없이 React Context API만 사용:

- **IdentifierProvider** — 참가 코드, 닉네임, QR URL (sessionStorage)
- **ParticipantsProvider** — 참가자 목록
- **WebSocketProvider** — STOMP 연결, 자동 재연결, 메시지 복구

## WebSocket 레이어 (`src/apis/websocket/`)

`@stomp/stompjs` + `sockjs-client` 기반 STOMP 프로토콜:

- `useWebSocketConnection` — 연결 라이프사이클
- `useWebSocketMessaging` — publish/subscribe
- `useWebSocketReconnection` — 구독 레지스트리 기반 자동 복구
- `useStompSessionWatcher` — 세션 상태 추적

## REST API 레이어 (`src/apis/rest/`)

fetch를 래핑한 커스텀 훅:

- `useFetch` — 즉시 GET
- `useLazyFetch` — 지연 GET
- `useMutation` — POST/PUT/PATCH/DELETE (에러 시 Toast 알림)

## 컴포넌트 계층

- `src/components/@common/` — 디자인 시스템 원자 컴포넌트 (Button, Modal, Toast 등)
- `src/components/@composition/` — 중간 조합 컴포넌트 (PlayerCard, ProbabilityList 등)
- `src/features/` — 기능별 페이지 및 로직
- `src/layouts/` — 공통 레이아웃 껍데기

스타일링은 **Emotion** CSS-in-JS. 스타일 파일은 `.styled.ts` 컨벤션. 디자인 토큰은 `src/styles/theme.ts`.

## 미니게임 패턴

각 미니게임은 `src/features/miniGame/<gameName>/` 하위에 동일한 구조를 가진다:

- `context/` — 게임별 상태 프로바이더
- `pages/` — Ready, Play, Result 페이지
- `components/` — 게임별 UI 컴포넌트

게임 설정은 `src/features/miniGame/config/gameConfigs.tsx`에 등록. `MiniGameProviders`가 `miniGameType` 파라미터를 보고 적절한 게임 컨텍스트를 주입한다.

## 인증 세션 복원 (`src/features/auth/contexts/AuthProvider.tsx`)

앱 로드 시 `bootstrap` 함수가 localStorage의 액세스 토큰 유무를 확인하고, 토큰이 있을 때만 `GET /users/me`로 세션을 복원한다. 토큰이 없으면 API 호출 없이 익명 상태로 시작한다.

토큰 저장소는 `LocalStorageTokenStore`(쿠키 아님). 과거 `CookieTokenStore` 사용 시절 "쿠키 존재 여부 판단 불가 → 무조건 시도" 방식의 흔적이 코드에 남지 않도록 주의한다.

## DevTools (`src/devtools/`, `ENABLE_DEVTOOLS`)

`process.env.ENABLE_DEVTOOLS`가 truthy일 때만 `DevToolsWrapper`(iframe 자동 테스트 패널, 네트워크 디버거)가 렌더링된다. `.env.development`에 `ENABLE_DEVTOOLS=true`가 설정되어 있어 로컬 dev 서버와 `build:dev`(be/dev 환경) 양쪽에서 활성화된다.

**기능별 가드도 `ENABLE_DEVTOOLS` 기준을 따른다.** `useMockMode`와 `useServiceWorkerUpdate`의 기능 활성화 조건은 `NODE_ENV === 'development'`가 아니라 `Boolean(process.env.ENABLE_DEVTOOLS)`로 통일되어 있다. `NODE_ENV`를 사용하면 `build:dev`처럼 `NODE_ENV=development`이지만 별도 배포 환경인 경우에도 동작하지만, `ENABLE_DEVTOOLS` 없이 `NODE_ENV=production` 빌드에 devtools를 붙이는 경우엔 기능이 비활성화된다. 두 조건을 일치시킴으로써 패널 노출 여부와 기능 동작 여부가 항상 같은 변수로 제어된다.

## 빌드

Vite가 아닌 **Webpack 5** 사용. 설정은 `webpack.common.js`, `webpack.dev.js`, `webpack.prod.js`로 분리. `@/*` → `src/*` path alias는 TypeScript와 Webpack 양쪽에 모두 설정됨. 환경변수는 빌드 타임에 `.env.development` / `.env.production`에서 로드.

Sentry는 `src/main.tsx`에서 프로덕션 전용으로 초기화되며, `@sentry/webpack-plugin`으로 소스맵 업로드가 통합되어 있다.

## 배포 인프라

**AWS CodePipeline → S3 Deploy** 구조. `buildspec.yml`이 CodeBuild에서 실행되며 `frontend/dist`를 아티팩트로 넘기면 CodePipeline이 S3에 배포한다. CloudFront가 S3 앞단에 위치한다.

- 정적 파일(`sitemap.xml`, `robots.txt`, `manifest.json` 등)은 `webpack.common.js`의 CopyWebpackPlugin으로 `dist/`에 복사되어 아티팩트에 포함된다.
