# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 역할

이 레포지토리에서 작업할 때는 **프론트엔드 시니어 개발자** 역할을 맡는다. 코드 리뷰, 아키텍처 결정, 구현 방향에 대해 경험 있는 프론트엔드 엔지니어 관점으로 조언하고 작업한다.

## 프로젝트 개요

**쫄(ZZOL)** 은 미니게임 기반 당첨자 추첨 서비스다. 단순 뽑기 대신 미니게임과 룰렛 시스템을 통해 당첨 확률에 직접 개입할 수 있는 "쫄깃한" 경험을 제공한다.

- 서비스: https://zzol.site
- 참가자는 QR코드로 방에 입장하고, 미니게임 결과에 따라 룰렛 당첨 확률이 결정된다.

## 문서

작업 기록 및 의사결정 배경은 `docs/` 폴더에 정리되어 있다. 특정 기능이나 설정의 맥락이 필요할 때 먼저 확인한다.

| 파일 | 내용 |
|------|------|
| `docs/home-tabs-architecture.md` | 홈 탭 구조 설계 (하단 탭바 구현 방식, 랭킹 카테고리 추가 방법, 건의사항 step 플로우) |
| `docs/tab-api-todo.md` | 홈 탭 관련 미구현 API 연동 가이드 (건의사항 POST /reports 등) |
| `docs/feature-backlog.md` | 구현 보류 중인 기능 목록 (게임 추가 게시판 등) |

## 커맨드

```bash
# 개발
npm run dev              # Webpack 개발 서버 (포트 3000)
npm run build:dev        # 개발 환경 빌드
npm run build            # 프로덕션 빌드

# 코드 품질
npm run lint             # ESLint 검사
npm run lint:fix         # ESLint 자동 수정
npm run format           # Prettier 포맷팅
npm run type-check       # TypeScript 타입 검사

# 테스트
npm run test:jest        # Jest 단위/컴포넌트 테스트
npm run test:jest -- --testPathPattern=<경로>  # 단일 테스트 파일 실행
npm run test:cypress     # Cypress E2E 테스트 실행
npm run storybook        # Storybook 개발 서버 (포트 6006)
npm run build-storybook  # Storybook 빌드 (CI 검증용)
```

### Storybook 주의사항

CI에서 Storybook 빌드 실패가 발생할 수 있다. `src/components/@common/` 또는 `src/components/@composition/` 하위 컴포넌트를 수정하거나 추가할 때는 관련 Story 파일(`.stories.tsx`)도 함께 확인하고, 로컬에서 `npm run build-storybook`으로 빌드 성공 여부를 검증한 후 PR을 올린다.

Node 버전은 `.nvmrc`에 명시 (22.18.0).

## 아키텍처

### 라우팅 (`src/router.tsx`)

React Router v7, 페이지는 모두 lazy-load. 주요 라우트 구조:
- `/` — 홈페이지
- `/entry/name` — 닉네임 입력 페이지
- `/room/:joinCode` — RoomLayout (WebSocket 컨텍스트 경계)
  - `/lobby` — 대기실
  - `/roulette/play`, `/roulette/result` — 룰렛 게임
  - `/:miniGameType/ready|play|result` — 미니게임 페이지 (MiniGameProviders로 래핑)
- `/join/:joinCode` — QR 딥링크 입장 페이지

### Provider 계층 (`src/App.tsx`)

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

### 상태 관리

외부 상태 라이브러리 없이 React Context API만 사용:
- **IdentifierProvider** — 참가 코드, 닉네임, QR URL (sessionStorage)
- **ParticipantsProvider** — 참가자 목록
- **WebSocketProvider** — STOMP 연결, 자동 재연결, 메시지 복구

### WebSocket 레이어 (`src/apis/websocket/`)

`@stomp/stompjs` + `sockjs-client` 기반 STOMP 프로토콜:
- `useWebSocketConnection` — 연결 라이프사이클
- `useWebSocketMessaging` — publish/subscribe
- `useWebSocketReconnection` — 구독 레지스트리 기반 자동 복구
- `useStompSessionWatcher` — 세션 상태 추적

### REST API 레이어 (`src/apis/rest/`)

fetch를 래핑한 커스텀 훅:
- `useFetch` — 즉시 GET
- `useLazyFetch` — 지연 GET
- `useMutation` — POST/PUT/PATCH/DELETE (에러 시 Toast 알림)

### 컴포넌트 계층

- `src/components/@common/` — 디자인 시스템 원자 컴포넌트 (Button, Modal, Toast 등)
- `src/components/@composition/` — 중간 조합 컴포넌트 (PlayerCard, ProbabilityList 등)
- `src/features/` — 기능별 페이지 및 로직
- `src/layouts/` — 공통 레이아웃 껍데기

스타일링은 **Emotion** CSS-in-JS. 스타일 파일은 `.styled.ts` 컨벤션. 디자인 토큰은 `src/styles/theme.ts`.

### 미니게임 패턴

각 미니게임은 `src/features/miniGame/<gameName>/` 하위에 동일한 구조를 가진다:
- `context/` — 게임별 상태 프로바이더
- `pages/` — Ready, Play, Result 페이지
- `components/` — 게임별 UI 컴포넌트

게임 설정은 `src/features/miniGame/config/gameConfigs.tsx`에 등록. `MiniGameProviders`가 `miniGameType` 파라미터를 보고 적절한 게임 컨텍스트를 주입한다.

### 빌드

Vite가 아닌 **Webpack 5** 사용. 설정은 `webpack.common.js`, `webpack.dev.js`, `webpack.prod.js`로 분리. `@/*` → `src/*` path alias는 TypeScript와 Webpack 양쪽에 모두 설정됨. 환경변수는 빌드 타임에 `.env.development` / `.env.production`에서 로드.

Sentry는 `src/main.tsx`에서 프로덕션 전용으로 초기화되며, `@sentry/webpack-plugin`으로 소스맵 업로드가 통합되어 있다.

### 배포 인프라

**AWS CodePipeline → S3 Deploy** 구조. `buildspec.yml`이 CodeBuild에서 실행되며 `frontend/dist`를 아티팩트로 넘기면 CodePipeline이 S3에 배포한다. CloudFront가 S3 앞단에 위치한다.

- 정적 파일(`sitemap.xml`, `robots.txt`, `manifest.json` 등)은 `webpack.common.js`의 CopyWebpackPlugin으로 `dist/`에 복사되어 아티팩트에 포함된다.
