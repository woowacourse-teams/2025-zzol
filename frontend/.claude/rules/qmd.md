## qmd 코드베이스 검색

qmd는 MCP 서버로 연결된 시맨틱 + 키워드 하이브리드 검색 도구다.

### 필수 사전 검색 — 새 코드 작성 전

**컴포넌트·훅·유틸·서비스를 새로 만들기 전에 반드시 qmd로 유사 코드가 있는지 확인한다.**
중복 구현을 막는 것이 qmd의 가장 중요한 역할이다.

```bash
qmd query "닉네임 최근 목록 저장" --collection zzol-fe
qmd query "토스트 에러 표시" --collection zzol-fe
```

결과에 유사 코드가 있으면 새로 만들지 않고 재사용한다.

### 컬렉션

| 컬렉션 | 대상 | 주요 용도 |
| --- | --- | --- |
| `zzol-fe` | `src/**/*.{ts,tsx}` (569개) | 컴포넌트, 훅, 유틸, Context 탐색 |
| `zzol-docs` | 백엔드 docs `**/*.md` (23개) | API 엔드포인트 스펙, WebSocket 토픽, 응답 타입 확인 |

### 언제 qmd vs Grep

| 상황 | 도구 |
| --- | --- |
| "이런 역할을 하는 코드가 있나?" | qmd query |
| "비슷한 패턴이 어디 있나?" | qmd query |
| "백엔드 API 스펙·응답 타입 확인" | qmd query `--collection zzol-docs` |
| 함수명·변수명·import 경로를 정확히 알 때 | Grep |
| 특정 문자열 사용처를 전부 셀 때 | Grep |
| qmd로 파일을 찾은 후 해당 파일에서 심볼 위치 특정 | qmd → Grep 순서 |

### 쿼리 작성 팁

- **한국어 쿼리**가 의도·개념 검색에 더 잘 맞는다. `qmd query`는 내부적으로 한국어를 영어로 자동 번역·확장하기 때문에 영어 코드베이스까지 커버된다. 영어로 입력하면 확장 방향이 의도와 멀어지는 경우가 있다.
- **정확한 심볼명**은 예외 — `qmd search`로 영어 그대로 쓴다: `qmd search "useWebSocketSubscription" --collection zzol-fe`
- **짧고 구체적**으로: 3–6어절이 가장 잘 동작한다
- 결과가 너무 많으면 `-n 3`으로 좁히고, 부족하면 `-n 10`으로 넓힌다 (기본값 5)

### 명령어

```bash
# 하이브리드 검색 (기본 권장)
qmd query "WebSocket 재연결 로직" --collection zzol-fe

# 키워드만 (심볼명·함수명)
qmd search "useMutation" --collection zzol-fe

# 백엔드 스펙 조회
qmd query "방 입장 API 요청 파라미터" --collection zzol-docs

# 결과 수 조정
qmd query "룰렛 확률 계산" --collection zzol-fe -n 3
```

### zzol-docs 활용 예시

백엔드 docs에는 게임별 WebSocket 스펙 문서가 있다. 신규 게임 연동 시 먼저 확인한다.

```bash
qmd query "사다리 게임 WebSocket 토픽" --collection zzol-docs
qmd query "블록 쌓기 게임 구독 경로" --collection zzol-docs
qmd query "룰렛 당첨자 응답 타입" --collection zzol-docs
```

### 인덱스 갱신

세션 시작 및 파일 저장 시 자동 갱신된다 (settings.json hook).
신규 파일 추가 후 즉시 검색이 안 되면:

```bash
qmd update   # 파일 목록 갱신
qmd embed    # 벡터 임베딩 누락 시 (검색 시 "N documents need embeddings" 경고가 뜨면 실행)
```
