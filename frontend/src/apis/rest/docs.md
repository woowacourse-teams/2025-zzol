# API 객체 사용법

> 프로젝트에서 HTTP API 통신을 위한 범용 유틸 함수 사용법

## 📁 파일 구조

```
src/
├── utils/
│   ├── api/
│   │   ├── error.ts      # 에러 클래스 정의
│   │   ├── apiRequest.ts    # API 요청 함수
│   │   └── api.ts        # API 요청 함수를 래핑한 객체 (GET, POST, PUT 등)
```

---

<br/>

## 📋 파일별 설명

### 1. `error.ts` - 에러 클래스

API 통신 중 발생할 수 있는 에러들을 타입별로 분류하여 처리합니다.

**포함된 에러 클래스:**

- `ApiError`: HTTP 상태 코드 에러 (400, 500 등)
- `NetworkError`: 네트워크 연결 실패 에러

<br/>

### 2. `apiRequest.ts` - API 요청 함수

HTTP 요청을 처리하는 핵심 함수가 포함되어 있습니다.

**주요 기능**

- 자동 JSON 직렬화/역직렬화
- 쿼리 파라미터 자동 처리
- 에러 응답 파싱
- 재시도 로직

<br/>

### 3. `api.ts` - API 요청 함수를 래핑한 객체

일반적인 HTTP 메서드들을 쉽게 사용할 수 있는 래퍼 함수들입니다.

<br/>

## ✅ 사용법

### GET 요청

```typescript
// 기본 GET 요청
const users = await api.get<User[]>('/api/users');

// 쿼리 파라미터와 함께
const filteredUsers = await api.get<User[]>('/api/users', {
  params: {
    page: 1,
    limit: 10,
    search: '김',
    active: true,
  },
});
// 실제 요청: /api/users?page=1&limit=10&search=김&active=true

// 커스텀 헤더
const protectedData = await api.get<any>('/api/protected', {
  headers: {
    Authorization: 'Bearer token123',
    'X-API-Version': 'v2',
  },
});
```

### POST/PUT/PATCH 요청

```typescript
// POST
const newUser = await api.post<User, CreateUserRequest>('/api/users', {
  name: '홍길동',
  email: 'hong@example.com',
});

// PUT
const updatedUser = await api.put<User, CreateUserRequest>('/api/users/1', {
  name: '김길동',
  email: 'kim@example.com',
});

// PATCH
const partiallyUpdated = await api.patch<User, Partial<CreateUserRequest>>('/api/users/1', {
  name: '박길동', // 이름만 수정
});
```

### DELETE 요청

```typescript
// 리소스 삭제
await api.delete<void>('/api/users/1');

// 쿼리 파라미터와 함께
await api.delete<void>('/api/users/1', {
  params: { force: true },
});
```

---

<br/>

## ⚙️ 추가 옵션

### 재시도 설정

네트워크 불안정이나 일시적 오류에 대비하여 자동 재시도를 설정할 수 있습니다.

```typescript
const importantData = await api.post<User, CreateUserRequest>('/api/users', userData, {
  retry: {
    count: 3, // 3번까지 재시도
    delay: 1000, // 1초 간격
  },
});
```

### 커스텀 헤더

요청별로 특별한 헤더가 필요한 경우 설정할 수 있습니다.

```typescript
const result = await api.post<any>('/api/upload', formData, {
  headers: {
    'X-Request-ID': 'unique-123',
    'Content-Type': 'multipart/form-data',
  },
});
```

<br/>

## 🚨 에러 처리

### 에러 타입별 처리

```typescript
try {
  const users = await api.get<User[]>('/api/users');
  return users;
} catch (error) {
  if (error instanceof ApiError) {
    // HTTP 에러 (400, 500 등)
    switch (error.status) {
      case 400:
        console.error('잘못된 요청:', error.message);
        break;
      case 401:
        console.error('인증 실패');
        // 로그인 페이지로 자동 리다이렉트
        break;
      case 403:
        console.error('권한 없음:', error.message);
        break;
      case 404:
        console.error('리소스를 찾을 수 없음');
        break;
      case 500:
        console.error('서버 오류:', error.message);
        break;
      default:
        console.error('API 오류:', error.message);
    }
  } else if (error instanceof NetworkError) {
    // 네트워크 연결 실패
    console.error('네트워크 오류:', error.message);
    // 사용자에게 인터넷 연결 확인 안내
  } else {
    console.error('알 수 없는 오류:', error);
  }
}
```
