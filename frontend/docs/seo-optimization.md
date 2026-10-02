# SEO 구성

검색 노출을 위해 프론트와 CloudFront에 둔 장치와 그 이유를 적는다. 작업 경위는 이슈(#1710)와 PR에 있다.

## 1. 라우트별 정적 HTML (#1710)

SPA 하나로는 색인 페이지가 홈 하나였다. 모든 경로가 canonical이 `/`인 같은 `index.html`을 받아 `/privacy`가 중복 페이지로 제외됐고, 없는 경로도 200을 돌려주는 soft 404가 났다.

지금은 `src/seo/pages.json` 하나에서 라우트별 HTML, `sitemap.xml`, `SeoContentPage` 본문이 만들어진다. 라우트별 HTML은 페이지마다 `HtmlWebpackPlugin`을 하나씩 둔다. 페이지는 `/`, `/guide`, `/games`, `/games/{slug}` 8종, `/privacy`, noindex인 `/404`다. 키워드 페이지를 더하려면 `pages.json`에 항목을 추가하면 된다. CI는 생성된 HTML과 sitemap의 `<loc>` 개수를 `pages.json`과 대조한다.

`public/index.html`은 템플릿이다. `<title>`·description·canonical·`og:url`·`robots`와 `#root` 안의 `<h1>`·`<p>`를 페이지마다 채운다. React가 `createRoot().render`로 `#root`를 교체하면 이 폴백 텍스트는 사라진다. 폴백 텍스트와 실제 화면 내용이 크게 다르면 cloaking 판정 위험이 있으니 서비스 소개 수준으로 유지한다.

## 2. 메타태그와 구조화 데이터

`public/index.html`에 `WebApplication` JSON-LD, `robots`, `theme-color`, `twitter:card`, `og:*`, `manifest` 링크가 있다. `keywords`·`author`·`hreflang`은 두지 않는다. Google이 쓰지 않고 과최적화 신호가 될 수 있다. description은 한 줄로 쓴다. content 안 줄바꿈은 검색 결과 snippet을 깨뜨린다.

`public/manifest.json`과 192·512·maskable 아이콘, `apple-touch-icon`으로 PWA로 인식된다.

## 3. sitemap.xml·robots.txt

`sitemap.xml`은 `webpack.common.js`의 인라인 플러그인이 `pages.json`에서 생성하고 `lastmod`를 빌드일로 채운다. `robots.txt`는 `public/`에서 복사한다.

CloudFront에는 `/sitemap.xml`, `/robots.txt` 전용 Cache Behavior가 있다. 없으면 4xx를 `index.html`로 보내는 Custom Error Response가 이 요청도 가로채 HTML을 돌려준다.

## 4. CloudFront Function `zzol-spa-router`

오리진이 S3 REST 엔드포인트라 `/guide` 요청은 객체 키 `guide`를 찾다 실패한다. `guide/index.html`이 있어도 자동으로 해석되지 않는다. 그래서 viewer-request 함수를 dev·prod 기본 동작에 붙였다.

```js
function handler(event) {
  var req = event.request;
  var uri = req.uri;
  if (/^\/(room|join|entry|auth)\//.test(uri)) {
    req.uri = '/index.html';
    return req;
  } // SPA 동적 라우트
  if (uri.endsWith('/')) req.uri = uri + 'index.html';
  else if (uri.lastIndexOf('.') <= uri.lastIndexOf('/')) req.uri = uri + '/index.html'; // 확장자 없는 URI
  return req;
}
```

파일이 없으면 기존 에러 응답이 4xx를 `/index.html` 200으로 받아준다. 없는 경로가 404를 돌려주려면 에러 응답을 `403/404 → /404/index.html, 응답코드 404`로 바꿔야 한다. 이 전환은 `404/index.html`이 S3에 있어야 하므로 dev → prod 순으로 적용한다.

도메인은 apex `zzol.site`가 GoDaddy 포워딩으로 `https://www.zzol.site`에 301 한 홉으로 간다. `dev.zzol.site`에는 응답 헤더 정책 `zzol-dev-noindex`를 붙여 `X-Robots-Tag: noindex, nofollow`를 내보낸다. prod와 중복 색인되지 않게 하려는 설정이다.

## 5. 배포 후 확인

`curl -sI`로 본다.

| URL                            | 기대                               |
| ------------------------------ | ---------------------------------- |
| `/games/card-game`             | 200, `<title>`·canonical이 자기 값 |
| `/privacy`, `/guide`, `/games` | 위와 동일                          |
| `/room/ABC/lobby`              | 200, `x-cache: Hit`, SPA           |
| `/aaa-not-exist`               | 에러 응답 전환 후 404              |
| `https://zzol.site/`           | 301 → `https://www.zzol.site`      |

## 6. 남은 일

- Google Search Console에 `www.zzol.site`와 sitemap이 등록돼 있다. "내기 게임"·"복불복 게임" 노출과 "커피내기 게임" 순위, CTR을 보고 다음 키워드 페이지를 정한다.
- Naver Search Advisor 등록은 네이버 유입이 필요해지면 한다.
- `WebApplication` JSON-LD의 `author`에 인라인된 Organization을 별도 `@type: Organization`으로 분리하고 `logo`·`sameAs`를 더하면 Knowledge Panel에 유리하다. SNS 계정 정비가 먼저다.
