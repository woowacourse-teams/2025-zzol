# CLAUDE.md

## 프로젝트 개요

**쫄(ZZOL)** — 미니게임 기반 당첨자 추첨 서비스. QR코드로 방 입장 후 미니게임 결과로 룰렛 당첨 확률 결정.

- 서비스: https://zzol.site

## 문서

특정 기능이나 설정의 맥락이 필요할 때 먼저 확인한다.

| 파일                             | 내용                                                                                     |
| -------------------------------- | ---------------------------------------------------------------------------------------- |
| `docs/architecture.md`           | 라우팅, Provider 계층, 상태관리, WebSocket, REST API, 컴포넌트 계층, 빌드, 배포          |
| `docs/todo/planned.md`           | 구현 예정 작업 목록                                                                      |
| `docs/todo/backlog.md`           | 방향 미확정 · 우선순위 보류 항목                                                         |
| `docs/todo/done.md`              | 완료된 작업 기록                                                                         |
| `docs/home-tabs-architecture.md` | 홈 탭 구조 설계 (하단 탭바 구현 방식, 랭킹 카테고리 추가 방법, 건의사항 step 플로우)     |
| `docs/api-design-menu-tab.md`    | 메뉴 탭 API 설계 — 백엔드 협의용 (POST /reports, GET /patch-notes Request/Response 스펙) |

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
