# SEO 최적화 작업 기록

## 문제 요약

`www.zzol.site/sitemap.xml` 접근 불가 및 SEO 누락 요소 다수 존재.

---

## 1. sitemap.xml 접근 불가

### 원인
CloudFront + S3 SPA 구조에서 4xx 오류를 모두 `index.html`로 포워딩하는 Custom Error Response 설정이 `/sitemap.xml` 요청도 가로채 HTML을 반환하고 있었음.

### 해결
CloudFront 배포에 `/sitemap.xml`, `/robots.txt` 경로에 대한 **별도 Cache Behavior 추가**. 해당 경로는 Custom Error Response를 거치지 않고 S3에서 직접 서빙.

---

## 2. sitemap.xml `lastmod` 하드코딩

### 원인
`public/sitemap.xml`의 `<lastmod>` 날짜가 고정값으로 작성되어 있어 배포 시 자동 갱신되지 않았음.

### 해결
`webpack.common.js`의 CopyWebpackPlugin `transform` 옵션으로 빌드 타임에 오늘 날짜로 자동 교체.

```js
transform(content) {
  const today = new Date().toISOString().split('T')[0];
  return content.toString().replace(/<lastmod>.*<\/lastmod>/g, `<lastmod>${today}</lastmod>`);
},
```

---

## 3. Web App Manifest 누락

### 원인
`manifest.json`이 없어 모바일 브라우저에서 PWA로 인식되지 않았고, 모바일 검색 노출에 불리한 상태.

### 해결
`public/manifest.json` 생성 및 `index.html`에 링크 추가. `webpack.common.js` CopyWebpackPlugin에 복사 패턴 추가.

> **후속 작업**: 192x192, 512x512 정사각형 PNG 아이콘을 `src/assets/logo/`에 추가하면 더 완성도 있는 PWA 지원 가능.

---

## 4. SEO 메타태그 누락 및 불일치

### 원인
- `canonical`, `og:url` URL에 trailing slash 없어 `sitemap.xml`과 불일치
- `hreflang` 태그 미설정으로 한국어 서비스임을 검색엔진에 명시하지 않음

### 해결
`public/index.html`에 아래 태그 추가 및 수정.

```html
<meta property="og:url" content="https://www.zzol.site/" />
<link rel="canonical" href="https://www.zzol.site/" />
<link rel="alternate" hreflang="ko" href="https://www.zzol.site/" />
<link rel="alternate" hreflang="x-default" href="https://www.zzol.site/" />
<link rel="manifest" href="/manifest.json" />
```

---

## 5. Google Search Console 등록

### 완료
- `www.zzol.site` 속성 등록 및 `https://www.zzol.site/sitemap.xml` 제출 완료.
- (선택) [Naver Search Advisor](https://searchadvisor.naver.com) 동일하게 등록 가능.

---

## 잔여 작업

| 항목 | 우선순위 | 비고 |
|---|---|---|
| Twitter Card 메타태그 추가 | 중 | X(트위터) 공유 미리보기 |
| JSON-LD 구조화 데이터 추가 | 중 | 검색 결과 리치 스니펫 |
| `apple-touch-icon` 추가 | 낮 | iOS 홈 화면 아이콘 |
| `theme-color` 메타태그 추가 | 낮 | 모바일 브라우저 상단 바 색상 |
| PWA 아이콘 192x192, 512x512 추가 | 낮 | `src/assets/logo/`에 정사각형 PNG 추가 필요 |
| Naver Search Advisor 등록 | 낮 | 네이버 검색 유입이 필요한 경우 |

---

## 변경 파일 목록

| 파일 | 변경 내용 |
|---|---|
| `public/index.html` | canonical·og:url trailing slash 통일, hreflang 추가, manifest 링크 추가 |
| `public/manifest.json` | 신규 생성 |
| `webpack.common.js` | manifest.json CopyPlugin 추가, sitemap lastmod 빌드 타임 자동 갱신 |
| CloudFront 설정 | `/sitemap.xml`, `/robots.txt` Cache Behavior 추가 (인프라 작업) |
