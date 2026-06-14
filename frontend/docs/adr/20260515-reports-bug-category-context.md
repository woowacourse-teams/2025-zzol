---
date: 2026-05-15
status: decided
---

# 건의사항 BUG 카테고리에만 gameType·joinCode 포함

## 맥락

POST /reports API 연동 시 카테고리별로 전송 데이터가 다르다. BUG 신고는 어떤 게임에서, 어떤 방에서 발생했는지가 핵심 디버깅 정보지만, SUGGESTION·GAME_REQUEST·OTHER에는 게임 컨텍스트가 무의미하다.

joinCode는 사용자가 마지막으로 입장한 방 코드로, `storageManager.getItem(STORAGE_KEYS.LAST_JOIN_CODE)` 에서 읽는다.

## 결정

`category === 'BUG'` 일 때만 `gameType`과 `joinCode`를 포함하고, 나머지 카테고리는 두 필드를 `null`로 전송한다.

## 고려한 대안

- **항상 모든 필드 포함**: SUGGESTION이나 GAME_REQUEST 신고에 방 코드·게임 타입은 의미 없는 데이터. 불필요한 전송.
- **카테고리별 별도 엔드포인트**: API 복잡도 증가. 단일 `POST /reports`로 충분히 처리 가능.

## 결과 및 영향

- `IdentifierProvider.clearIdentifier()` 호출 시 `LAST_JOIN_CODE`를 localStorage에 저장해야 joinCode 조회가 가능하다.
- BUG 카테고리 UI에서만 게임 선택 필 섹션(`S.GameSection`)이 렌더된다.
- gameType은 사용자가 선택하지 않으면 null로 전송된다 (선택 사항).
