import webpack from 'webpack';
import CopyWebpackPlugin from 'copy-webpack-plugin';
import HtmlWebpackPlugin from 'html-webpack-plugin';
import dotenv from 'dotenv';
import { readFileSync } from 'fs';
import path, { dirname } from 'path';
import { fileURLToPath } from 'url';
import { sentryWebpackPlugin } from '@sentry/webpack-plugin';
import WebpackBundleAnalyzer from 'webpack-bundle-analyzer';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const packageJson = JSON.parse(readFileSync(path.resolve(__dirname, 'package.json'), 'utf8'));
const appVersion = packageJson.version;

const SITE_URL = 'https://www.zzol.site';

// 라우트별 메타의 단일 소스. SPA 콘텐츠 페이지(src/seo/pages.ts)와 같은 파일을 읽는다.
const seoPages = JSON.parse(readFileSync(path.resolve(__dirname, 'src/seo/pages.json'), 'utf8'));

// 홈(`/`)을 뺀 콘텐츠 라우트의 최상위 세그먼트. webpack.prod.js 의 Service Worker
// denylist 가 이걸 쓴다 — 라우트 목록을 두 벌로 적으면 pages.json 에 라우트를 더할 때 조용히 어긋난다.
// Workbox 는 pathname 이 아니라 pathname+search 에 매칭하므로 끝을 `[/?]` 까지 허용한다.
export const CONTENT_ROUTE_PATTERN = new RegExp(
  `^/(${[...new Set(seoPages.filter((page) => page.path !== '/').map((page) => page.path.split('/')[1]))].join('|')})($|[/?])`
);

// 라우트마다 실제 파일을 만들어 둘 뿐이다. S3 REST 오리진은 `/guide` 를 `guide/index.html` 로 해석하지 않으므로
// 확장자 없는 URI 를 `.../index.html` 로 재작성하는 CloudFront Function(`zzol-spa-router`)이 dev·prod 에 붙어 있다.
// 함수·에러 응답 설정은 docs/seo-optimization.md §7 참고.
const htmlPlugins = (devSnippet) =>
  seoPages.map(
    (page) =>
      new HtmlWebpackPlugin({
        template: './public/index.html',
        filename: page.path === '/' ? 'index.html' : `${page.path.slice(1)}/index.html`,
        templateParameters: {
          DEV_SNIPPET: devSnippet,
          TITLE: page.title,
          DESCRIPTION: page.description,
          CANONICAL: `${SITE_URL}${page.path}`,
          H1: page.h1,
          BODY: page.body,
          ROBOTS: page.noindex ? 'noindex, follow' : 'index, follow, max-image-preview:large',
          // `<` 를 이스케이프한다 — 값에 `</script>` 가 섞이면 스크립트가 그 자리에서 끊긴다.
          JSON_LD: JSON.stringify(
            page.jsonLd ?? {
              '@context': 'https://schema.org',
              '@type': 'WebPage',
              name: page.title,
              description: page.description,
              url: `${SITE_URL}${page.path}`,
              inLanguage: 'ko',
              isPartOf: { '@type': 'WebSite', name: '쫄 (ZZOL)', url: `${SITE_URL}/` },
            }
          ).replace(/</g, '\\u003c'),
        },
      })
  );

// sitemap 도 같은 소스에서 만든다 — public/sitemap.xml 수기 관리를 없애고 lastmod 를 빌드일로 갱신한다.
const sitemapPlugin = {
  apply(compiler) {
    compiler.hooks.thisCompilation.tap('SitemapPlugin', (compilation) => {
      compilation.hooks.processAssets.tap(
        {
          name: 'SitemapPlugin',
          stage: compiler.webpack.Compilation.PROCESS_ASSETS_STAGE_ADDITIONAL,
        },
        () => {
          const lastmod = new Date().toISOString().split('T')[0];
          const urls = seoPages
            .filter((page) => !page.noindex)
            .map(
              (page) =>
                `  <url>\n    <loc>${SITE_URL}${page.path}</loc>\n    <lastmod>${lastmod}</lastmod>\n` +
                `    <changefreq>${page.changefreq}</changefreq>\n    <priority>${page.priority}</priority>\n  </url>`
            )
            .join('\n');

          compilation.emitAsset(
            'sitemap.xml',
            new compiler.webpack.sources.RawSource(
              `<?xml version="1.0" encoding="UTF-8"?>\n` +
                `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`
            )
          );
        }
      );
    });
  },
};

export default (_, argv) => {
  const mode = argv.mode || 'development';

  dotenv.config({ path: path.resolve(process.cwd(), `.env.${mode}`) }).parsed || {};

  // .env.development 는 gitignore 대상이라 워크트리에는 없을 수 있다(#1660).
  // 기본값이 없으면 번들에 undefined 가 박혀 REST 요청이 `undefined/...` 로 나가고,
  // 실패가 요청 시점까지 미뤄져 원인이 드러나지 않는다. webpack.dev.js 가 devServer
  // 프록시에 쓰는 기본값과 같은 값으로 맞춰 두 설정의 비대칭을 없앤다.
  const devApiUrl = mode === 'development' ? 'http://localhost:8080' : undefined;

  const envKeys = {
    'process.env.NODE_ENV': JSON.stringify(mode),
    'process.env.VERSION': JSON.stringify(appVersion),
    'process.env.API_URL': JSON.stringify(process.env.API_URL || devApiUrl),
    'process.env.ENABLE_DEVTOOLS': JSON.stringify(process.env.ENABLE_DEVTOOLS === 'true'),
    // 값이 없어도 반드시 치환한다 — 조건부로 두면 번들에 process.env.DSN_KEY가
    // 리터럴로 남아 브라우저에서 ReferenceError(process is not defined)로 앱이 죽는다.
    'process.env.DSN_KEY': JSON.stringify(process.env.DSN_KEY || ''),
  };
  if (process.env.SENTRY_AUTH_TOKEN) {
    envKeys['process.env.SENTRY_AUTH_TOKEN'] = JSON.stringify(process.env.SENTRY_AUTH_TOKEN);
  }

  return {
    mode,
    entry: './src/main.tsx',
    output: {
      publicPath: '/',
      path: path.resolve(__dirname, 'dist'),
      filename: '[name].[contenthash].js',
      chunkFilename: '[name].[contenthash].chunk.js',
      clean: true,
    },
    module: {
      rules: [
        { test: /\.tsx?$/, use: 'ts-loader', exclude: /node_modules/ },
        {
          test: /\.(png|svg|jpg|jpeg|gif|webp)$/i,
          type: 'asset/resource',
          generator: {
            filename: (pathData) => {
              // assets/logo 폴더의 이미지들은 해시값 없이 원본 이름 유지
              if (pathData.filename.includes('assets/logo/')) {
                return 'logo/[name][ext]';
              }
              // 다른 이미지들은 기존처럼 해시값 포함
              return '[name].[contenthash][ext]';
            },
          },
        },
        { test: /\.css$/i, use: ['style-loader', 'css-loader'] },
      ],
    },
    resolve: {
      extensions: ['.tsx', '.ts', '.js'],
      alias: { '@': path.resolve(__dirname, 'src') },
      conditionNames: ['import', 'module', 'browser', 'default'],
    },
    plugins: [
      ...htmlPlugins(
        process.env.ENABLE_DEVTOOLS === 'true'
          ? `<script type="module" src="/devtools/devSnippet.js"></script>`
          : ''
      ),
      sitemapPlugin,
      new CopyWebpackPlugin({
        patterns: [
          {
            from: 'public/fonts',
            to: 'fonts',
          },
          {
            from: 'src/assets/logo',
            to: 'logo',
          },
          {
            from: 'public/robots.txt',
            to: 'robots.txt',
          },
          {
            // HtmlWebpackPlugin 의 favicon 옵션 대신 여기서 복사한다 —
            // 페이지 수만큼 인스턴스가 생기면 같은 파일을 여러 번 emit 하게 된다.
            from: 'public/favicon.ico',
            to: 'favicon.ico',
          },
          {
            from: 'public/manifest.json',
            to: 'manifest.json',
          },
          {
            from: 'public/icons',
            to: 'icons',
          },
          ...(process.env.ENABLE_DEVTOOLS === 'true'
            ? [
                {
                  from: 'public/devtools',
                  to: 'devtools',
                },
              ]
            : []),
        ],
      }),
      new webpack.DefinePlugin(envKeys),
      sentryWebpackPlugin({
        authToken: process.env.SENTRY_AUTH_TOKEN,
        org: 'woowacourse-7th-fe',
        project: '2025-zzol',
        release: appVersion,
        sourcemaps: { disable: mode !== 'production' },
      }),
      new WebpackBundleAnalyzer.BundleAnalyzerPlugin({
        analyzerMode: 'static',
        openAnalyzer: false,
        reportFilename: 'bundle-report.html',
      }),
    ],
    devServer: {
      static: {
        directory: path.resolve(__dirname, 'dist'),
      },
      compress: true,
      port: 3000,
      hot: true,
      open: true,
      historyApiFallback: true,
    },

    optimization: {
      usedExports: true,
      sideEffects: false,

      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          vendor: {
            test: /[\\/]node_modules[\\/]/,
            name: 'vendors',
            chunks: 'all',
          },
        },
      },
    },
  };
};
