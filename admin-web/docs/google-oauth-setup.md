# 구글 ID 토큰 로그인 설정

백오피스는 구글 로그인으로만 들어간다. 브라우저가 구글에서 **ID 토큰**을 받아 서버에 넘기면,
서버가 검증한 뒤 자체 관리자 JWT를 발급한다. 이 문서는 그 준비 절차다.

**소요 시간 약 15분.** 구글 클라이언트 없이 들어가는 우회 경로는 없다. 로컬에서도 실제 구글 로그인을 쓴다.

---

## 0. 전체 그림

```text
[1] admin-web            구글 로그인 팝업 → ID 토큰 (구글이 서명한 JWT)
[2] POST /admin/api/auth/login  { idToken }
[3] 서버                  구글 JWKS로 서명 검증
                         iss = https://accounts.google.com 확인
                         aud = 우리 클라이언트 ID 확인
                         email_verified = true 확인
                         허용목록(ADMIN_EMAILS ∪ admin_account) 대조
[4] 서버 → admin-web      관리자 JWT (type=ADMIN, sub=email, 1시간)
                         + refresh 토큰 HttpOnly 쿠키
[5] 이후 모든 요청         Authorization: Bearer <관리자 JWT>
[6] 401 이면              POST /admin/api/auth/refresh 로 재발급 후 한 번 더 보냄
```

토큰 수명과 재발급 규칙은 [`../CLAUDE.md`](../CLAUDE.md)의 "토큰 보관" 절이 정한다. 여기서는 되풀이하지 않는다.

**구글 계정으로 로그인했다고 관리자가 되는 것이 아니다.** 구글은 "이 사람이 이 이메일의
주인이 맞다"까지만 증명하고, 관리자 여부는 우리 허용목록이 정한다.

---

## 1. 구글 클라우드 프로젝트 선택

<https://console.cloud.google.com> 접속 후 상단 프로젝트 선택기에서 프로젝트를 고른다.

기존 서비스 로그인(google/kakao/naver)에 쓰는 프로젝트가 이미 있다면 **같은 프로젝트를
쓰되 클라이언트는 새로 만든다**(2절 참고). 프로젝트가 없으면 새로 만든다.

---

## 2. OAuth 클라이언트 ID 발급

좌측 메뉴 **API 및 서비스 → 사용자 인증 정보(Credentials)** 로 간다.

**+ 사용자 인증 정보 만들기 → OAuth 클라이언트 ID** 를 누른다.

| 항목 | 값 |
| --- | --- |
| 애플리케이션 유형 | **웹 애플리케이션** |
| 이름 | `zzol-admin` (구분되게) |
| 승인된 자바스크립트 원본 | `http://localhost:5173`<br>`https://admin.zzol.site`<br>`https://admin.dev.zzol.site` |
| 승인된 리디렉션 URI | **비워 둔다** |

### 도메인

| 환경 | 도메인 |
| --- | --- |
| 로컬 | `http://localhost:5173` (Vite 기본 포트) |
| dev | `https://admin.dev.zzol.site` |
| prod | `https://admin.zzol.site` |

dev 는 **`admin.dev.zzol.site`** 다. 나머지 dev 주소가 `dev.zzol.site`, `dev.api.zzol.site` 처럼
환경을 한 레벨로 떼어 놓고 있어 그쪽에 맞췄다.

**인증서를 따로 챙겨야 한다.** `*.zzol.site` 와일드카드는 한 레벨만 매칭하므로
`admin.dev.zzol.site` 를 덮지 못한다. `*.dev.zzol.site` 를 따로 발급하거나 ACM SAN 에 이 주소를
직접 넣는다. 하이픈(`dev-admin.zzol.site`)이면 같은 레벨이라 기존 와일드카드로 끝났겠지만,
주소 체계의 일관성을 택했다.

dev 와 prod 를 한 클라이언트에 함께 등록했다. `aud` 검증은 <b>환경 경계가 아니라 서비스 경계</b>를
지키는 장치이고(서비스 로그인용 토큰이 백오피스로 넘어오는 것을 막는 것), 환경 사이의 실제 경계는
각 환경의 `ADMIN_EMAILS`다. dev 테스터를 prod 에서 확실히 막아야 할 일이 생기면 그때 클라이언트를
나누고 `ADMIN_GOOGLE_CLIENT_ID`만 환경별로 달리 주면 된다. 서버 코드는 바뀌지 않는다.

### 왜 서비스 로그인용과 분리하나

기존 클라이언트를 재사용하면 `aud` 검증이 무의미해진다. 서비스 로그인으로 발급된
아무 사용자의 ID 토큰이 백오피스 로그인 엔드포인트에서 `aud` 검증을 통과해 버린다.
그다음 방어선이 허용목록 하나뿐이 되므로, 클라이언트를 나눠 한 겹 더 둔다.

### 왜 리디렉션 URI가 없나

리디렉션 방식(Authorization Code)이 아니라 **팝업에서 ID 토큰만 받는 방식**이기 때문이다.
서버가 구글에 코드를 교환하러 가지 않으므로 **client secret도 쓰지 않는다.**
클라이언트 ID만 있으면 된다. 발급 화면에 secret이 같이 나오지만 이 경로에서는 쓰지 않는다.

### 승인된 원본 규칙

- **스킴과 포트까지 정확히** 일치해야 한다. `http://localhost:5173`과 `http://localhost:5174`는 다른 값이다.
- 경로(`/login` 등)를 붙이면 안 된다. 오리진만 넣는다.
- 반영에 몇 분 걸릴 수 있다. 방금 추가했는데 `origin_mismatch`가 뜨면 잠시 뒤 다시 시도한다.

발급이 끝나면 **클라이언트 ID**(`....apps.googleusercontent.com`)를 복사한다.

---

## 3. OAuth 동의 화면

**API 및 서비스 → OAuth 동의 화면**으로 간다.

| 항목 | 값 |
| --- | --- |
| User Type | 조직 구글 워크스페이스가 있으면 **내부**, 없으면 **외부** |
| 앱 이름 | `ZZOL 백오피스` |
| 사용자 지원 이메일 | 본인 |
| 범위(Scopes) | `openid`, `email`, `profile` 세 개만 |

**외부**를 골랐고 앱이 "테스트" 상태라면 **테스트 사용자**에 관리자 이메일을 넣어야 한다.
넣지 않으면 로그인 시 `access_denied`가 난다.

백오피스는 소수만 쓰므로 **테스트 상태로 두는 편이 낫다.** 게시(프로덕션 전환)하면
구글 검토 대상이 되는데, 여기서 요구하는 범위는 검토가 필요 없는 기본 범위지만
굳이 공개 상태로 만들 이유가 없다. 테스트 상태에서 테스트 사용자는 100명까지 등록된다.

---

## 4. 환경변수

### 백엔드

| 변수 | 값 | 설명 |
| --- | --- | --- |
| `ADMIN_EMAILS` | `mj04300017@gmail.com` | 부트스트랩 허용목록. 쉼표 구분. **UI로 삭제 불가** |
| `ADMIN_GOOGLE_CLIENT_ID` | `....apps.googleusercontent.com` | 2절에서 복사한 값 |
| `ADMIN_JWT_SECRET` | 32자 이상 ASCII | 선택. 미설정 시 `JWT_SECRET`으로 폴백 |

설정 위치는 `backend/app/src/main/resources/config/security.yml`의 `admin.auth` 블록이고,
로컬은 `backend/.env`에 넣는다(워크트리에는 심볼릭 링크로 걸려 있다).

`admin.auth.web-origins`는 프로필별 `application-{profile}.yml`에 있는, refresh 와 logout 을 부를 수 있는 오리진 목록이다.
admin-web 도메인이 여기 없으면 로그인은 되는데 1시간 뒤 재발급이 거절된다. 새 도메인을 붙이면 CORS 허용 목록과
이 목록에 둘 다 넣는다.

`ADMIN_JWT_SECRET`을 굳이 나누면 좋은 이유: 관리자 토큰은 권한이 훨씬 크다.
사용자 JWT 시크릿이 새더라도 관리자 토큰까지 위조되지는 않게 분리해 두는 편이 안전하다.
다만 미설정이어도 `type=ADMIN` 클레임 검증이 있어 사용자 토큰이 관리자로 통과하지는 않는다.

### 프론트

| 변수 | 값 |
| --- | --- |
| `VITE_GOOGLE_CLIENT_ID` | 백엔드와 **같은** 클라이언트 ID |
| `VITE_API_BASE_URL` | 로컬은 비움(프록시), 배포는 `https://api.zzol.site` |
| `VITE_ENV_NAME` | `LOCAL`/`DEV`/`PROD`. 레일 머리의 환경 배지에 찍힌다 |

클라이언트 ID는 비밀이 아니다. 브라우저에 그대로 노출되는 값이고, 실제 방어는
승인된 원본과 서버측 `aud`, 허용목록이 한다. Vite는 `VITE_*`를 **빌드 시점에 번들에
인라인**하므로 배포 시 build-arg나 CI 변수로 넘겨야 한다.

---

## 5. 프론트 구현 방식

구글 Identity Services 가 그린 버튼에서 ID 토큰을 받고, 서버 응답으로 로그인 상태를 세운다. 구현은 `src/auth/`에 있다.

| 파일 | 역할 |
| --- | --- |
| `google.ts` | GSI 스크립트 로드, 버튼 렌더, `credential` 콜백 |
| `AuthProvider.tsx` | `/auth/login`으로 교환하고 `/auth/me`로 상태 확정, 로그아웃 |
| `refresh.ts` | 401 뒤 refresh 쿠키로 재발급. 탭 사이 경합은 Web Locks |
| `../api/client.ts` | 모든 요청의 Bearer 부착과 401 재시도 |

**받는 것은 `credential`(ID 토큰)이지 access token이 아니다.** 구글 API를 호출할 게
없으므로 access token은 필요 없다.

로그인·refresh·logout 요청은 `credentials: 'include'`로 보낸다.

---

## 6. 확인 절차

로컬 백엔드 `:8080`과 admin-web `:5173`을 띄우고 브라우저에서 구글 로그인을 마친 뒤 확인한다.

```bash
# 1. 로그인 뒤 개발자 도구 Network 에서 POST /admin/api/auth/login 응답 → 200 + accessToken,
#    Set-Cookie 에 zzol_admin_refresh (HttpOnly, Path=/admin/api/auth)

# 2. 허용목록에 없는 구글 계정으로 로그인 → 403 NOT_ADMIN. 화면은 로그인 페이지에 머문다

# 3. 발급받은 토큰으로 본인 확인 → 200 {"email":"..."}
curl http://localhost:8080/admin/api/auth/me -H "Authorization: Bearer $TOKEN"

# 4. 토큰 없이 → 401
curl -i http://localhost:8080/admin/api/auth/me

# 5. 허용되지 않은 오리진에서 재발급 → 403 ADMIN_ORIGIN_NOT_ALLOWED. 쿠키가 있어도 web-origins 가 먼저다
curl -i -X POST http://localhost:8080/admin/api/auth/refresh -H 'Origin: http://evil.site'
```

---

## 7. 자주 걸리는 곳

| 증상 | 원인 |
| --- | --- |
| `origin_mismatch` | 승인된 자바스크립트 원본에 현재 오리진이 없다. 포트와 스킴까지 확인한다. 방금 추가했다면 몇 분 기다린다 |
| `access_denied` | 동의 화면이 외부 + 테스트 상태인데 테스트 사용자에 그 이메일이 없다 |
| 로그인은 되는데 서버가 401 | `ADMIN_GOOGLE_CLIENT_ID`와 `VITE_GOOGLE_CLIENT_ID`가 다르다. `aud` 검증에서 걸린다 |
| 서버가 403 `NOT_ADMIN` | 구글 인증은 통과했는데 `ADMIN_EMAILS`에 없다. 값 반영에 앱 재시작이 필요하다 |
| `GOOGLE_EMAIL_UNVERIFIED` | 그 구글 계정의 이메일이 검증되지 않았다. 드물다 |
| 앱이 기동 실패 | `ADMIN_JWT_SECRET`도 `JWT_SECRET`도 없거나 32자 미만이다 |
| 전원 로그인 불가 | `ADMIN_EMAILS`에 유효한 계정을 넣고 재배포한다. 이 목록은 UI로 지울 수 없는 break-glass 경로다 |
| 로그인은 되는데 1시간 뒤 다시 로그인 화면 | 그 환경의 `admin.auth.web-origins`에 admin-web 오리진이 없다. refresh 가 거절된다 |
