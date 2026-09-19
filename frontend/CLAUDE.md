# CLAUDE.md

## 명령어

```bash
npm run dev          # webpack dev server
npm run lint:fix     # eslint --fix (코드 변경 후 실행)
npm run type-check   # tsc --noEmit
npm run test:jest    # jest
npm run storybook    # Storybook :6006
npm run build-storybook   # @common/@composition 수정 시 PR 전 검증
npm run generate:ws       # ws-openapi.json → wsOpenApi.d.ts (BE 계약이 바뀐 뒤)
```

## WebSocket 계약은 BE 가 생성한 타입이 정한다

`src/apis/websocket/generated/` 의 세 파일(`wsContract.ts`·`ws-openapi.json`·`wsOpenApi.d.ts`)은 손으로 고치지 않는다. destination 과 payload 타입은 BE 애노테이션과 record 에서 생성되고, 카탈로그에 없는 경로나 어긋난 필드는 `tsc` 가 잡는다. 필드가 필요하거나 타입이 틀렸다고 느껴지면 FE 에서 타입을 고치는 게 아니라 **BE PR 을 선행**한다.

- 어떻게 쓰는가: `.claude/skills/ws-contract/`(생성 파일에서 destination 찾기, 훅 시그니처, 없는 경로 요청 절차)
- 구조와 갱신 명령: `docs/architecture.md` "계약 타입"
- 왜 이렇게 하는가: `docs/adr/20260915-ws-contract-generated-types.md`

## 문서

특정 기능이나 설정의 맥락이 필요할 때 먼저 확인한다.

| 파일 | 내용 |
| --- | --- |
| `docs/architecture.md` | 라우팅, Provider 계층, 상태관리, WebSocket, REST API, 컴포넌트 계층, 빌드, 배포 |
| `docs/adr/` | 설계 결정 기록 — 결정 근거·대안·영향 (ADR) |
| `docs/block-stacking.md` | 블록 쌓기 미니게임 설계 |
| `docs/seo-optimization.md` | SEO 최적화 작업 기록 |
| `docs/api-design-menu-tab.md` | 메뉴 탭 API 설계 — 백엔드 협의용 (POST /reports, GET /patch-notes Request/Response 스펙) |
| `docs/adr/20260915-ws-contract-generated-types.md` | WebSocket 계약을 BE 생성 타입으로 강제하는 결정이 FE 에 미치는 영향. 결정 본문은 `backend/docs/adr/0037` |

## .claude 리소스

> 브랜치 전략·git push 안전·공통 스킬(`create-issue`·`create-pr`·`adr`)은 **모노레포 루트**로 통합됐다. 루트 [CLAUDE.md](../CLAUDE.md)·[.claude/rules/git-push-safety.md](../.claude/rules/git-push-safety.md)·[.claude/skills/](../.claude/skills/) 참조. 아래는 프론트 전용 리소스만 정리한다.

`.claude/rules/`는 자동 로드되고(`paths` 범위가 있는 파일은 해당 경로 작업 시에만), `.claude/skills/`는 `paths`에 해당하는 파일 작업 시 proactive하게 적용한다.

### Skills

| 스킬 | 언제 뜨는가 | 설명 |
| --- | --- | --- |
| `ws-contract` | `src/apis/websocket/**`, `src/contexts/**`, `use*WebSocket*`·`use*Subscription*` 훅 | 구독·발행 코드를 쓰기 전에 생성 파일에서 destination 과 payload 를 확인한다. 없는 경로는 BE PR 을 선행한다 |

### Agents

| 에이전트 | 설명 |
| --- | --- |
| `fe-code-reviewer` | zzol FE 고유 규칙(컴포넌트 계층·스타일 토큰·API 훅·WebSocket 컨트랙트·a11y·Storybook·ADR) 감수. 범용 버그·중복·효율은 다루지 않음. 항상 `run_in_background: true`로 실행 |

#### 코드 리뷰 = 두 도구 병행

"코드 리뷰해줘" 같은 요청은 **두 도구를 함께** 돌린다. 역할이 다르므로 한쪽만으로는 부족하다.

| 도구 | 담당 | 호출 |
| --- | --- | --- |
| `/code-review` (내장 커맨드) | 범용 버그(정확성)·중복·단순화·효율, 일반 React/TS 정확성. effort·`ultra`(클라우드)·`--comment`·`--fix` 지원 | **사용자가 직접 입력** — Claude는 `Skill("code-review")`로 호출할 수 없다 |
| `fe-code-reviewer` (에이전트) | zzol FE 고유 규칙·ADR 준수 (`/code-review`가 모르는 영역) | Agent 툴, `run_in_background: true` |

실행 패턴: Claude는 `fe-code-reviewer`를 백그라운드로 띄워 FE 고유 규칙을 보고, 범용 버그 렌즈가 필요하면 `Skill("deep-review")`를 함께 돌린다. `/code-review`는 사용자가 직접 입력했을 때만 도는 커맨드이므로 Claude의 실행 계획에 넣지 않는다(근거: `backend/.claude/rules/agent-dispatch.md`). (자세한 분업은 `.claude/agents/fe-code-reviewer.md` "검토 범위" 참조)
