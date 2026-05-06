## qmd 코드베이스 검색

qmd는 MCP 서버로 연결된 시맨틱 + 키워드 하이브리드 검색 도구다.
파일 경로나 심볼을 모를 때, 또는 "이런 역할을 하는 코드"를 찾을 때 Grep보다 먼저 사용한다.

### 컬렉션

| 컬렉션      | 대상                         | 용도                              |
| ----------- | ---------------------------- | --------------------------------- |
| `zzol-fe`   | `frontend/src/**/*.{ts,tsx}` | 컴포넌트, 훅, 유틸, 컨텍스트 검색 |
| `zzol-docs` | `backend/docs/**/*.md`       | 백엔드 API 스펙, 설계 문서 검색   |

### 언제 qmd를 쓰는가

**qmd 우선** — 의도·개념 기반 탐색:

- "이미 비슷한 훅이 있는지" 확인할 때
- 특정 기능이 어느 컴포넌트에 구현됐는지 찾을 때
- API 엔드포인트의 스펙이나 사용 예시를 문서에서 찾을 때
- 넓은 범위를 한 번에 탐색해야 할 때

**Grep 우선** — 정확한 심볼·문자열 검색:

- 함수명, 변수명, import 경로를 정확히 알 때
- 특정 문자열이 몇 군데 쓰이는지 셀 때

### 사용 방법

```
# 추천: 하이브리드 검색 (의미 + 키워드 자동 조합)
qmd query "WebSocket 재연결 로직" --collection zzol-fe

# 벡터 유사도만
qmd vsearch "토스트 알림 컴포넌트" --collection zzol-fe

# 키워드만 (빠름)
qmd search "useMutation" --collection zzol-fe

# 결과 줄 수 조정
qmd query "룰렛 확률 계산" --collection zzol-fe -n 5
```

### 인덱스 갱신

세션 시작 시 자동으로 `qmd update`가 실행된다 (settings.json SessionStart hook).
수동으로 강제 갱신이 필요하면:

```
qmd update
```

컬렉션 상태 확인:

```
qmd collection list
```
