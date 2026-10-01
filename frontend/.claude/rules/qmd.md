## qmd 코드베이스 검색

qmd는 시맨틱 + 키워드 하이브리드 검색 CLI다. 인덱스는 `settings.json` 훅이 세션 시작과 `.ts`·`.tsx` 저장 때 `qmd update`로 갱신한다.

### 필수 사전 검색 — 새 코드 작성 전

**컴포넌트·훅·유틸·서비스를 새로 만들기 전에 반드시 qmd로 유사 코드가 있는지 확인한다.** 중복 구현을 막는 것이 qmd의 가장 중요한 역할이다.

```bash
qmd query "닉네임 최근 목록 저장" --collection zzol-fe
qmd search "useWebSocketSubscription" --collection zzol-fe   # 정확한 심볼명은 search
```

결과에 유사 코드가 있으면 새로 만들지 않고 재사용한다.

### 컬렉션

| 컬렉션      | 대상                 | 주요 용도                        |
| ----------- | -------------------- | -------------------------------- |
| `zzol-fe`   | `src/**/*.{ts,tsx}`  | 컴포넌트, 훅, 유틸, Context 탐색 |
| `zzol-docs` | 백엔드 `docs/**/*.md` | REST API 스펙, ADR, 설계 문서 확인 |

WebSocket destination과 payload 타입은 문서가 아니라 `src/apis/websocket/generated/`의 생성 파일로 확인한다. 절차는 `.claude/skills/ws-contract/SKILL.md`에 있다.

### 언제 qmd vs Grep

| 상황                                     | 도구                               |
| ---------------------------------------- | ---------------------------------- |
| "이런 역할을 하는 코드가 있나?"          | qmd query                          |
| "백엔드 API 스펙·설계 배경 확인"         | qmd query `--collection zzol-docs` |
| 함수명·변수명·import 경로를 정확히 알 때 | Grep                               |
| 특정 문자열 사용처를 전부 셀 때          | Grep                               |

### 쿼리 작성 팁

- **한국어 쿼리**가 의도·개념 검색에 더 잘 맞는다. `qmd query`가 한국어를 영어로 번역·확장하므로 영어 코드베이스까지 찾는다.
- **정확한 심볼명**은 `qmd search`로 영어 그대로 쓴다.
- **짧고 구체적**으로 쓴다. 3~6어절이 가장 잘 동작한다.
- 결과 수는 `-n`으로 조정한다. 기본값은 5다.

### 인덱스 갱신

신규 파일 추가 후 즉시 검색이 안 되면 수동으로 갱신한다.

```bash
qmd update   # 파일 목록 갱신
qmd embed    # 검색 시 "N documents need embeddings" 경고가 뜨면 실행
```
