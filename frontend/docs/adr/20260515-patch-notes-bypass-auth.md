---
date: 2026-05-15
status: decided
---

# 패치 노트 훅에서 useFetch 대신 api.get() 직접 호출

## 맥락

`GET /patch-notes` 와 `GET /patch-notes/latest` 는 비로그인 사용자도 접근해야 하므로 `bypassAuth: true` 옵션이 필요하다. `useFetch`는 이 옵션을 지원하지 않는다.

## 결정

`usePatchNotes.ts` 에서 `useFetch` 대신 `api.get()` 을 `bypassAuth: true` 와 함께 직접 호출한다.

## 고려한 대안

- **useFetch 사용**: bypassAuth 미지원으로 불가.
- **useFetch에 bypassAuth 추가**: 공통 훅 변경은 파급 범위가 크다. 패치 노트 단일 케이스를 위한 범위 확장은 과함.

## 결과 및 영향

- `api-conventions` 규칙(컴포넌트·훅에서 api 직접 호출 금지)의 예외 케이스다. `bypassAuth`가 필요한 비인증 공개 API는 동일하게 `api.get()`을 훅 내에서 직접 사용한다.
- 컴포넌트(`PatchNotesView`)는 훅만 사용하며 `api`를 직접 import하지 않는다. 훅 경계 안에서만 직접 호출이 허용된다.
- 백엔드 응답의 `createdAt`이 초 단위 Unix timestamp와 ISO 문자열 둘 다 가능하므로 `formatPatchNoteDate` 유틸이 두 형식을 모두 처리한다 (`< 1e12` 조건으로 구분).
