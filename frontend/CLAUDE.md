# CLAUDE.md

## 프로젝트 개요

**쫄(ZZOL)** — 미니게임 기반 당첨자 추첨 서비스. QR코드로 방 입장 후 미니게임 결과로 룰렛 당첨 확률 결정.

- 서비스: https://zzol.site

## 문서

특정 기능이나 설정의 맥락이 필요할 때 먼저 확인한다.

| 파일 | 내용 |
| --- | --- |
| `docs/architecture.md` | 라우팅, Provider 계층, 상태관리, WebSocket, REST API, 컴포넌트 계층, 빌드, 배포 |
| `docs/adr/` | 설계 결정 기록 — 결정 근거·대안·영향 (ADR) |
| `docs/block-stacking.md` | 블록 쌓기 미니게임 설계 |
| `docs/seo-optimization.md` | SEO 최적화 작업 기록 |
| `docs/api-design-menu-tab.md` | 메뉴 탭 API 설계 — 백엔드 협의용 (POST /reports, GET /patch-notes Request/Response 스펙) |

## API 컨트랙트 조회 (MCP)

BE 컨트랙트(WebSocket + HTTP)를 직접 `curl` 로 받아도 되지만, 본 레포는 `tools/api-mcp/` MCP 서버를 통해 Claude Code 에서 바로 조회한다. WebSocket 은 `GET /dev/ws-catalog`, HTTP 는 springdoc OpenAPI(`GET /v3/api-docs`)를 소비한다. (구 `ws-mcp` 에서 HTTP/OpenAPI 도구가 추가되며 `api-mcp` 로 통합됨)

| 도구 | 용도 |
| --- | --- |
| `ws_list_topics` | 전체 토픽/큐/send 목록 + path/description substring 검색 |
| `ws_describe` | 특정 path 의 풀 컨트랙트 (payloadType + publishers + 참조 schema) |
| `ws_source` | 특정 path 의 발행 메서드 위치 (className#methodName) |
| `ws_connect` / `ws_subscribe` / `ws_send` | STOMP 세션을 짧게 띄워 연결/구독/송신 검증 (`roomToken` 필요 — ADR-0009 참조) |
| `http_list_endpoints` | OpenAPI 엔드포인트 요약 목록 (method/path/summary) |
| `http_describe` | 특정 엔드포인트의 요청/응답 스키마 상세 |
| `http_request` | 실제 백엔드로 REST 요청 후 `{ request, response }` 반환 |
| `http_validate` | 바디를 OpenAPI 스키마와 대조한 누락/타입 불일치 리포트 |

**등록**: `frontend/.mcp.json` 에 이미 정의되어 있다(서버 키 `api`). `cd frontend && claude` 로 띄우면 자동 인식.

**MCP 빌드**: 별도 빌드 불필요. `frontend/.mcp.json` 이 self-healing 런처(`../tools/api-mcp/scripts/launch.mjs`)를 가리키므로 실행 시점에 의존성 설치·빌드를 자동 보장한다.

**컨트랙트 검증 위치**: api-mcp 의 zod 스키마와 BE 카탈로그의 일치(contract drift) 검증은 **be/dev 가 단독으로 소유**한다 — fixture 생성기(`WsCatalogFixtureGeneratorTest`, `-DupdateFixture=true`)·커밋된 fixture·BE 소스가 모두 be/dev 에 있기 때문이다. `tools/api-mcp` 는 be/dev 미러이므로 fe/dev CI 는 컨트랙트 검증을 수행하지 않고 빌드·린트·단위 테스트만 돌린다.

**prefix 주의사항**: MCP 카탈로그의 path 는 prefix 를 포함(`/topic/room/...`, `/user/queue/...`, `/app/...`)하지만, FE 의 `useWebSocketSubscription`/`send` 는 wrapper 가 prefix 를 자동 추가하므로 path 에서 `/topic`·`/app` 부분을 제거해 전달한다 (자세한 규칙은 `.claude/rules/websocket.md`).

상세 도구 명세·환경 변수·동작 검증(MCP Inspector) 은 `../tools/api-mcp/README.md` 참조.

## .claude 리소스

### Rules

자동 로드된다. 경로 범위가 있는 파일은 해당 경로 작업 시에만 활성화된다.

| 파일 | 범위 | 내용 |
| --- | --- | --- |
| `principles.md` | 전역 | 역할, 코딩 원칙, 외과적 변경, 작업 원칙 |
| `git-push-safety.md` | 전역 | 보호 브랜치(fe/dev·fe/prod·main 등) 직접 push 금지, upstream·refspec 검증 절차 |
| `style.md` | 전역 | 색상·타이포그래피·z-index 토큰, Emotion 패턴 금지 항목 |
| `qmd.md` | 전역 | 코드베이스 시맨틱 검색 사용법 |
| `websocket.md` | `src/apis/websocket/**`, `src/contexts/**` | WebSocket 구독·발행 패턴, destination 형식 |
| `feature-structure.md` | `src/features/**` | Feature 슬라이스 디렉터리 구조, 레이어 역할 |

### Skills

`paths`에 해당하는 파일 작업 시 proactive하게 적용한다.

| 스킬 | 트리거 경로 | 내용 |
| --- | --- | --- |
| `api-conventions` | `src/features/**/hooks/**`, `src/apis/**` | useFetch·useLazyFetch·useMutation 선택 기준·사용 패턴 |
| `storybook-conventions` | `src/components/@common/**`, `@composition/**` | Story 파일 구조, variant 커버리지, 금지 패턴 |
| `minigame-structure` | `src/features/miniGame/**`, `src/contexts/*Game*/**` | 게임 디렉터리 구조, Context 위치, GAME_CONFIGS 등록 절차 |
| `style-check` | `**/*.styled.ts` | 스타일 규칙 위반 자동 검증·수정 |

### Commands

| 커맨드 | 설명 |
| --- | --- |
| `/adr` | 코드 작업 전 설계 결정 논의 및 ADR 문서 저장 |
| `/create-pr` | PR 템플릿 기반 GitHub Pull Request 생성 |
| `/style-audit` | 변경 파일 전체 스타일 규칙 일괄 감사 |

### Agents

| 에이전트 | 설명 |
| --- | --- |
| `fe-code-reviewer` | zzol FE 고유 규칙(컴포넌트 계층·스타일 토큰·API 훅·WebSocket 컨트랙트·a11y·Storybook·ADR) 감수. 범용 버그·중복·효율은 다루지 않음. 항상 `run_in_background: true`로 실행 |

#### 코드 리뷰 = 두 도구 병행

"코드 리뷰해줘" 같은 요청은 **두 도구를 함께** 돌린다. 역할이 다르므로 한쪽만으로는 부족하다.

| 도구 | 담당 | 호출 |
| --- | --- | --- |
| `/code-review` (내장 스킬) | 범용 버그(정확성)·중복·단순화·효율, 일반 React/TS 정확성. effort·`ultra`(클라우드)·`--comment`·`--fix` 지원 | Skill 툴 |
| `fe-code-reviewer` (에이전트) | zzol FE 고유 규칙·ADR 준수 (`/code-review`가 모르는 영역) | Agent 툴, `run_in_background: true` |

실행 패턴: `fe-code-reviewer`를 백그라운드로 먼저 띄우고 `/code-review`를 돌린 뒤, 두 결과를 합쳐 보고한다. (자세한 분업은 `.claude/agents/fe-code-reviewer.md` "검토 범위" 참조)

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
