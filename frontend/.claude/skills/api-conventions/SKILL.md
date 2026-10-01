---
description: API 훅(useFetch, useLazyFetch, useMutation)을 작성하거나 수정할 때 zzol REST API 레이어 컨벤션을 자동 적용한다. 컴포넌트에서 api.* 직접 호출 금지.
paths:
  - 'src/features/**/hooks/**'
  - 'src/features/**/pages/**'
  - 'src/apis/**'
allowed-tools: Read
---

# API 레이어 컨벤션

## 훅 선택 기준

| 상황                              | 훅             |
| --------------------------------- | -------------- |
| 컴포넌트 마운트 시 자동 조회      | `useFetch`     |
| 버튼 클릭 등 사용자 액션으로 조회 | `useLazyFetch` |
| POST / PUT / PATCH / DELETE       | `useMutation`  |

`api.get()`, `api.post()` 등을 컴포넌트나 훅 내에서 **직접 호출하지 않는다**. 예외는 `bypassAuth: true`가 필요한 비인증 공개 API 하나다. 훅은 이 옵션을 받지 않으므로 훅 경계 안에서만 `api.get()`을 직접 부른다([ADR](../../../docs/adr/20260515-patch-notes-bypass-auth.md)). `api.*` 옵션은 `src/apis/rest/docs.md`에 있다.

---

## useFetch — 자동 실행 GET

```ts
import useFetch from '@/apis/rest/useFetch';

const { data, loading, refetch } = useFetch<ResponseType>({
  endpoint: '/api/resource',
  enabled: isReady,              // 기본값 true — false면 실행 안 함
  errorDisplayMode: 'toast',     // 'fallback' | 'toast' | 'text' (생략 시 ErrorBoundary로 throw)
  onSuccess: (data) => { ... },
  onError: (error) => { ... },
});
```

- `enabled`가 `false`이면 마운트 시 실행되지 않는다
- `errorDisplayMode` 미지정 시 에러가 `throw`되어 ErrorBoundary가 처리한다

---

## useLazyFetch — 수동 실행 GET

```ts
import useLazyFetch from '@/apis/rest/useLazyFetch';

const { data, loading, execute } = useLazyFetch<ResponseType>({
  endpoint: '/api/resource',
  errorDisplayMode: 'toast',
  onSuccess: (data) => { ... },
});

// 사용자 액션에서 호출
const handleLoad = async () => {
  await execute();
};
```

---

## useMutation — CUD 요청

```ts
import useMutation from '@/apis/rest/useMutation';

const { mutate, loading } = useMutation<ResponseType, RequestType>({
  endpoint: '/api/resource',
  method: 'POST',                // 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  errorDisplayMode: 'toast',     // useMutation은 필수
  onSuccess: (data, variables) => { ... },
  onError: (error, variables) => { ... },
});

const handleSubmit = async (payload: RequestType) => {
  await mutate(payload);
};
```

- `errorDisplayMode`는 `useMutation`에서 **필수**다 (타입상 required)
- DELETE는 `variables`가 `void`이므로 `await mutate()` 로 호출

---

## errorDisplayMode 결정 기준

| 값           | 동작                                                                                   | 사용 시점             |
| ------------ | -------------------------------------------------------------------------------------- | --------------------- |
| `'toast'`    | 에러 토스트를 띄우고 `error`를 `null`로 비운다                                          | 대부분의 경우         |
| `'text'`     | 토스트도 throw도 없다. 훅의 `error`는 `null`로 비워지므로 `onError`로 받아 직접 그린다 | 폼 인라인 에러        |
| `'fallback'` | throw해서 `LocalErrorBoundary`가 fallback UI를 그린다                                  | 영역 단위 에러 처리   |
| 미지정       | throw해서 ErrorBoundary가 처리한다. `'fallback'`과 같다                                | 페이지 레벨 에러 처리 |

세 값 모두 `onError`는 호출된다.

---

## 엔드포인트 관리

- 엔드포인트 문자열은 훅 파일 안의 상수로 둔다. 쿼리 파라미터도 이 문자열에 직접 붙인다
- 컴포넌트에 하드코딩하지 않는다

```ts
// ✅
const ENDPOINT = '/api/rooms' as const;
const { data } = useFetch<Room[]>({ endpoint: ENDPOINT });

// ❌
const { data } = useFetch<Room[]>({ endpoint: '/api/rooms' });
```
