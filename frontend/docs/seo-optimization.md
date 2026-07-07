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

| 항목                      | 우선순위 | 비고                           |
| ------------------------- | -------- | ------------------------------ |
| Naver Search Advisor 등록 | 낮       | 네이버 검색 유입이 필요한 경우 |

> `apple-touch-icon`, PWA 아이콘(192x192, 512x512, maskable) 모두 완료됨.

---

## 변경 파일 목록

| 파일                   | 변경 내용                                                               |
| ---------------------- | ----------------------------------------------------------------------- |
| `public/index.html`    | canonical·og:url trailing slash 통일, hreflang 추가, manifest 링크 추가 |
| `public/manifest.json` | 신규 생성                                                               |
| `webpack.common.js`    | manifest.json CopyPlugin 추가, sitemap lastmod 빌드 타임 자동 갱신      |
| CloudFront 설정        | `/sitemap.xml`, `/robots.txt` Cache Behavior 추가 (인프라 작업)         |

---

## 6. 검색 키워드 확장 최적화 (2026-05-14)

### 배경

Google Search Console 기준 "커피내기 게임", "커피 내기 게임", "커피 내기" 평균 순위 8위권.
"내기 게임", "복불복 게임" 등 인접 키워드로 확장이 필요한 상황.

### 진단한 문제

1. **meta description 줄바꿈 버그** — content 속성 내 줄바꿈으로 검색 결과 snippet이 깨져 CTR 손해
2. **JSON-LD 없음** — Google이 JS 실행 없이 서비스 성격을 파악할 수단 부재
3. **SPA 구조 한계** — 초기 HTML 본문이 비어있어 키워드 연관성 신호가 약함
4. **GA script self-closing 버그** — `<script ... />`는 HTML에서 self-closing 불가, gtag 초기화 미실행

### 해결

**`public/index.html`**

- meta description / og:description 줄바꿈 제거, "내기 게임" 키워드 자연스럽게 포함
- `keywords`, `author`, `hreflang alternate` 제거 (Google 미사용, over-optimization 신호 위험)
- `WebApplication` JSON-LD 구조화 데이터 추가 — `featureList`는 기능 설명 문장으로 작성
- `robots`, `theme-color`, `twitter:card` 메타태그 추가
- `#root` 안에 React 마운트 전 크롤러용 폴백 콘텐츠 추가 (React가 교체하면 자동 소멸)
- GA script self-closing 버그 수정

#### 폴백 콘텐츠 방식 근거

`public/index.html`의 `#root` 안에 정적 텍스트를 넣는 방식. React가 `ReactDOM.render()`로 `#root`를 교체하면 자동으로 사라져 별도 JS 처리나 hidden text 없이 구현 가능.

```html
<div id="root">
  <main style="...">
    <h1>쫄 — 커피 내기·점심 내기를 미니게임으로</h1>
    <p>내기 게임, 복불복 게임을 더 재밌게!...</p>
  </main>
</div>
```

> **주의**: 폴백 텍스트와 React 앱 실제 화면 내용이 크게 다르면 Google cloaking 판정 위험. 서비스 소개 수준으로 유지할 것.

---

## 다음 단계 (우선순위 순)

### 단기 — Search Console 데이터 모니터링

이번 변경 배포 후 **2~4주** Google Search Console에서 아래 항목을 확인한다.

- "내기 게임", "복불복 게임" 노출 여부
- "커피내기 게임" 순위 변화 (8위 → 상위권)
- CTR 변화 (description 수정 효과)

데이터를 보고 추가 작업 여부를 결정한다.

### 중기 — 콘텐츠 SEO (가이드 페이지)

Search Console 데이터에서 순위 개선이 더딜 경우 고려.

- `/guide` 같은 별도 정적 콘텐츠 페이지 신설
- 키워드별 타겟 페이지: "내기 게임이란", "커피 내기 방법", "미니게임으로 당번 정하기"
- **주의**: React 라우트로 만들면 SPA 문제가 동일하게 적용됨 → 정적 HTML 파일 or Prerender 필요

### 장기 — Prerender 또는 SSG

근본적인 SPA SEO 한계를 해결하려면 사전 렌더링이 필요하다.

| 방식            | 설명                                                                         | 공수 |
| --------------- | ---------------------------------------------------------------------------- | ---- |
| **Prerender**   | 기존 SPA 유지. 빌드 후 Puppeteer가 HTML 스냅샷 저장 (`prerender-spa-plugin`) | 중   |
| **Next.js SSG** | Next.js 마이그레이션. 빌드 타임에 완성된 HTML 생성                           | 높음 |

현재 인덱싱 대상 페이지가 홈 하나라 Prerender가 현실적. Next.js 마이그레이션은 서비스 규모가 커지거나 다중 랜딩 페이지가 필요해지는 시점에 검토.

### 보류 — Organization JSON-LD 분리

현재 `WebApplication` JSON-LD의 `author` 안에 Organization이 인라인으로 포함되어 있음. 별도 `@type: Organization` JSON-LD로 분리하고 `logo`, `sameAs`(SNS 링크)를 추가하면 Google Knowledge Panel 노출에 유리하나, SNS 계정 정비가 선행되어야 의미 있음.
