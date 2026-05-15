# CLAUDE.md

## 프로젝트 개요

**쫄(ZZOL)** — 미니게임 기반 당첨자 추첨 서비스. QR코드로 방 입장 후 미니게임 결과로 룰렛 당첨 확률 결정.

- 서비스: https://zzol.site

## 문서

특정 기능이나 설정의 맥락이 필요할 때 먼저 확인한다.

| 파일 | 내용 |
| --- | --- |
| `docs/architecture.md` | 라우팅, Provider 계층, 상태관리, WebSocket, REST API, 컴포넌트 계층, 빌드, 배포 |
| `docs/todo/planned.md` | 구현 예정 작업 목록 |
| `docs/todo/backlog.md` | 방향 미확정 · 우선순위 보류 항목 |
| `docs/todo/done.md` | 완료된 작업 기록 |
| `docs/block-stacking.md` | 블록 쌓기 미니게임 설계 |
| `docs/seo-optimization.md` | SEO 최적화 작업 기록 |
| `docs/api-design-menu-tab.md` | 메뉴 탭 API 설계 — 백엔드 협의용 (POST /reports, GET /patch-notes Request/Response 스펙) |

## .claude 리소스

### Rules

자동 로드된다. 경로 범위가 있는 파일은 해당 경로 작업 시에만 활성화된다.

| 파일 | 범위 | 내용 |
| --- | --- | --- |
| `principles.md` | 전역 | 역할, 코딩 원칙, 외과적 변경, 작업 원칙 |
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
| `/create-pr` | PR 템플릿 기반 GitHub Pull Request 생성 |
| `/style-audit` | 변경 파일 전체 스타일 규칙 일괄 감사 |

### Agents

| 에이전트 | 설명 |
| --- | --- |
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
