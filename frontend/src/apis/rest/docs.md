# `api` 객체 사용법

`src/apis/rest/`의 저수준 HTTP 함수 설명이다. 컴포넌트와 훅은 이 객체를 직접 부르지 않고 `useFetch`·`useLazyFetch`·`useMutation`을 쓴다. 훅 사용법과 예외는 `.claude/skills/api-conventions/SKILL.md`에 있다.

| 파일            | 역할                                                                 |
| --------------- | -------------------------------------------------------------------- |
| `error.ts`      | HTTP 상태 코드 에러 `ApiError`, 연결 실패 `NetworkError`, `ErrorDisplayMode` |
| `apiRequest.ts` | `fetch` 래퍼. JSON 직렬화, 에러 파싱, 재시도, 401 토큰 갱신 후 1회 재요청 |
| `api.ts`        | `apiRequest`를 메서드별로 감싼 `api.get`·`post`·`put`·`patch`·`delete` |

## 호출

```ts
const users = await api.get<User[]>('/users?page=1&limit=10');
const created = await api.post<User, CreateUserRequest>('/users', { name: '홍길동' });
await api.put<User, CreateUserRequest>('/users/1', { name: '김길동' });
await api.patch<User, Partial<CreateUserRequest>>('/users/1', { name: '박길동' });
await api.delete<void>('/users/1');
```

- URL은 `API_URL` 뒤에 그대로 붙는다. 쿼리 파라미터는 문자열에 직접 쓴다.
- body는 항상 `JSON.stringify`로 보낸다. FormData는 보낼 수 없다.
- 204나 빈 응답은 `{}`로 돌아온다.

## 옵션

```ts
await api.get<PatchNote[]>('/patch-notes', {
  bypassAuth: true,                   // Authorization 헤더와 401 갱신을 생략한다
  headers: { 'X-Request-ID': 'abc' }, // 기본 헤더에 덧붙인다
  retry: { count: 3, delay: 1000 },   // 실패 시 재시도 횟수와 간격(ms)
  errorDisplayMode: 'toast',          // 던지는 에러의 displayMode. 기본은 GET 'fallback', 그 외 'toast'
});
```

## 에러

실패하면 `ApiError` 또는 `NetworkError`를 던진다. 둘 다 `displayMode`를 가지며, `LocalErrorBoundary`는 `'fallback'`인 에러만 잡고 나머지는 위로 올린다. 서버가 `application/problem+json`을 주면 `detail`이 `message`가 되고 원문은 `data`에 남는다.

```ts
try {
  return await api.get<User[]>('/users');
} catch (error) {
  if (error instanceof ApiError) console.error(error.status, error.message);
  else if (error instanceof NetworkError) console.error('네트워크 오류', error.message);
  throw error;
}
```
