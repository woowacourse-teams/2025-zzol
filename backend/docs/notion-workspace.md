# Notion 워크스페이스 참조

ZZOL 프로젝트의 Notion 워크스페이스 구조와 주요 페이지 위치를 정리한다.

## 워크스페이스 루트

[ZZOL(쫄?) 프로젝트](https://www.notion.so/22920b2652778094a4efc61e83182dfb)

## 주요 페이지

| 페이지 | URL | 설명 |
|---|---|---|
| API 명세서 | https://www.notion.so/23b20b2652778060bb12ff8b1f0edb45 | WebSocket · REST API 스펙 통합 페이지 |
| WebSocket 명세서 DB | https://www.notion.so/23e20b26527780898fb7de01a9f75b62 | 게임별 WebSocket 엔드포인트 목록 |
| BE | https://www.notion.so/22920b26527780dcbdf7f4a431120177 | 백엔드 개발 문서 (Swagger 링크 포함) |
| INFRA | https://www.notion.so/23120b265277803eb11ff49161aa0522 | 인프라 설정 및 배포 관련 문서 |

## WebSocket 명세서 DB 구조

`카테고리` 옵션: `CARDGAME` · `BLOCKSTACKING` · `SPEEDTOUCH` · `LADDERGAME` · `ROOM`

새 게임의 WebSocket 스펙을 추가할 때 이 DB에 행을 추가한다.

- **API 명**: 엔드포인트 경로 (예: `[subscribe] /topic/room/{joinCode}/ladder/state`)
- **카테고리**: 게임 종류
- **설명**: 용도 한 줄 요약
- **선택**: 수정 필요 여부 (`변경필요` / `수정중`)

## API 스펙 작업 흐름

1. `docs/` 에 게임별 WebSocket 스펙 Markdown 작성 (로컬 개발 참조용)
2. Notion WebSocket 명세서 DB에 엔드포인트별 행 추가 (팀 공유용)
3. 두 문서를 동기화 상태로 유지한다

## Notion MCP 연동

Claude Code에서 Notion MCP 서버를 통해 워크스페이스를 직접 읽고 쓸 수 있다.

### 설정

`~/.claude/settings.json` 에 아래 블록을 추가한다.
Notion API 토큰은 [Notion 인테그레이션 설정](https://www.notion.so/profile/integrations)에서 발급한다.

```json
{
  "mcpServers": {
    "notion": {
      "command": "npx",
      "args": ["-y", "@notionhq/notion-mcp-server"],
      "env": {
        "OPENAPI_MCP_HEADERS": "{\"Authorization\": \"Bearer <NOTION_TOKEN>\", \"Notion-Version\": \"2022-06-28\"}"
      }
    }
  }
}
```

인테그레이션을 생성한 뒤, 접근할 페이지/DB에서 **연결(Connect)** 을 통해 인테그레이션을 추가해야 한다.

### 주요 도구

| 도구 | 용도 |
|---|---|
| `notion_retrieve_page` | 페이지 블록 내용 조회 |
| `notion_retrieve_database` | DB 스키마(속성) 조회 |
| `notion_query_database` | DB 행 목록 조회 (필터·정렬 지원) |
| `notion_create_page` | 페이지 또는 DB 행 생성 |
| `notion_append_block_children` | 기존 페이지에 블록 추가 |
| `notion_update_page` | 페이지 속성(제목·카테고리 등) 수정 |
| `notion_search` | 제목 기준 페이지·DB 전체 검색 |

### 활용 패턴

**WebSocket 명세 DB에 새 엔드포인트 행 추가**

```text
notion_create_page
  parent: { database_id: "23e20b26527780898fb7de01a9f75b62" }
  properties:
    API 명: "[subscribe] /topic/room/{joinCode}/ladder/state"
    카테고리: "LADDERGAME"
    설명: "사다리 게임 현재 상태 구독"
```

**DB 전체 행 목록 확인**

```text
notion_query_database
  database_id: "23e20b26527780898fb7de01a9f75b62"
  filter: { property: "카테고리", select: { equals: "LADDERGAME" } }
```
