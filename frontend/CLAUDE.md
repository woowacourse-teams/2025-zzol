# CLAUDE.md

## 프로젝트 개요

**쫄(ZZOL)** — 미니게임 기반 당첨자 추첨 서비스. QR코드로 방 입장 후 미니게임 결과로 룰렛 당첨 확률 결정.

- 서비스: https://zzol.site

## 문서

특정 기능이나 설정의 맥락이 필요할 때 먼저 확인한다.

| 파일                          | 내용                                                                                     |
| ----------------------------- | ---------------------------------------------------------------------------------------- |
| `docs/architecture.md`        | 라우팅, Provider 계층, 상태관리, WebSocket, REST API, 컴포넌트 계층, 빌드, 배포          |
| `docs/adr/`                   | 설계 결정 기록 — 결정 근거·대안·영향 (ADR)                                               |
| `docs/block-stacking.md`      | 블록 쌓기 미니게임 설계                                                                  |
| `docs/seo-optimization.md`    | SEO 최적화 작업 기록                                                                     |
| `docs/api-design-menu-tab.md` | 메뉴 탭 API 설계 — 백엔드 협의용 (POST /reports, GET /patch-notes Request/Response 스펙) |

## WebSocket 컨트랙트 조회 (MCP)

BE 의 `GET /dev/ws-catalog` 에 모든 토픽/큐/send destination 의 path·payloadType·publisher 위치가 노출되어 있다. 직접 `curl` 로 받아도 되지만, 본 레포는 `tools/ws-mcp/` MCP 서버를 통해 Claude Code 에서 바로 조회한다.

| 도구                                      | 용도                                                                          |
| ----------------------------------------- | ----------------------------------------------------------------------------- |
| `ws_list_topics`                          | 전체 토픽/큐/send 목록 + path/description substring 검색                      |
| `ws_describe`                             | 특정 path 의 풀 컨트랙트 (payloadType + publishers + 참조 schema)             |
| `ws_source`                               | 특정 path 의 발행 메서드 위치 (className#methodName)                          |
| `ws_connect` / `ws_subscribe` / `ws_send` | STOMP 세션을 짧게 띄워 연결/구독/송신 검증 (`roomToken` 필요 — ADR-0009 참조) |

**등록**: `frontend/.mcp.json` 에 이미 정의되어 있다. `cd frontend && claude` 로 띄우면 자동 인식.

**MCP 빌드**: 최초 1회 `cd ../tools/ws-mcp && npm install && npm run build` 가 필요하다. 이후 BE 시그니처가 변경되면 BE 측에서 `WsCatalogFixtureExportTest` 가 fixture 를 갱신하고 MCP CI 가 zod 스키마 일치를 자동 검증한다.

**prefix 주의사항**: MCP 카탈로그의 path 는 prefix 를 포함(`/topic/room/...`, `/user/queue/...`, `/app/...`)하지만, FE 의 `useWebSocketSubscription`/`send` 는 wrapper 가 prefix 를 자동 추가하므로 path 에서 `/topic`·`/app` 부분을 제거해 전달한다 (자세한 규칙은 `.claude/rules/websocket.md`).

상세 도구 명세·환경 변수·동작 검증(MCP Inspector) 은 `../tools/ws-mcp/README.md` 참조.

## .claude 리소스

### Rules

자동 로드된다. 경로 범위가 있는 파일은 해당 경로 작업 시에만 활성화된다.

| 파일                   | 범위                                       | 내용                                                                           |
| ---------------------- | ------------------------------------------ | ------------------------------------------------------------------------------ |
| `principles.md`        | 전역                                       | 역할, 코딩 원칙, 외과적 변경, 작업 원칙                                        |
| `git-push-safety.md`   | 전역                                       | 보호 브랜치(fe/dev·fe/prod·main 등) 직접 push 금지, upstream·refspec 검증 절차 |
| `style.md`             | 전역                                       | 색상·타이포그래피·z-index 토큰, Emotion 패턴 금지 항목                         |
| `qmd.md`               | 전역                                       | 코드베이스 시맨틱 검색 사용법                                                  |
| `websocket.md`         | `src/apis/websocket/**`, `src/contexts/**` | WebSocket 구독·발행 패턴, destination 형식                                     |
| `feature-structure.md` | `src/features/**`                          | Feature 슬라이스 디렉터리 구조, 레이어 역할                                    |

### Skills

`paths`에 해당하는 파일 작업 시 proactive하게 적용한다.

| 스킬                    | 트리거 경로                                          | 내용                                                     |
| ----------------------- | ---------------------------------------------------- | -------------------------------------------------------- |
| `api-conventions`       | `src/features/**/hooks/**`, `src/apis/**`            | useFetch·useLazyFetch·useMutation 선택 기준·사용 패턴    |
| `storybook-conventions` | `src/components/@common/**`, `@composition/**`       | Story 파일 구조, variant 커버리지, 금지 패턴             |
| `minigame-structure`    | `src/features/miniGame/**`, `src/contexts/*Game*/**` | 게임 디렉터리 구조, Context 위치, GAME_CONFIGS 등록 절차 |
| `style-check`           | `**/*.styled.ts`                                     | 스타일 규칙 위반 자동 검증·수정                          |

### Commands

| 커맨드         | 설명                                         |
| -------------- | -------------------------------------------- |
| `/adr`         | 코드 작업 전 설계 결정 논의 및 ADR 문서 저장 |
| `/create-pr`   | PR 템플릿 기반 GitHub Pull Request 생성      |
| `/style-audit` | 변경 파일 전체 스타일 규칙 일괄 감사         |

### Agents

| 에이전트           | 설명                                                                                             |
| ------------------ | ------------------------------------------------------------------------------------------------ |
| `fe-code-reviewer` | 컴포넌트 계층·스타일링·훅 설계·접근성 기준 독립 코드 리뷰. 항상 `run_in_background: true`로 실행 |

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

Node 버전: `.nvmrc` 참고 (22.18.0)
