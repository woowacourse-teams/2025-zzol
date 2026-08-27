import { merge } from 'webpack-merge';
import { GenerateSW } from 'workbox-webpack-plugin';
import common from './webpack.common.js';

export default (env, argv) =>
  merge(common(env, { ...argv, mode: 'production' }), {
    devtool: 'source-map',
    plugins: [
      new GenerateSW({
        clientsClaim: true,
        skipWaiting: true,
        navigateFallback: '/index.html',
        // 라우트별 정적 HTML 이 있는 콘텐츠 경로는 SW 가 홈 HTML 로 대체하면 안 된다
        // — 재방문자에게 홈 title/canonical 이 보이면 정적 생성이 무의미해진다.
        navigateFallbackDenylist: [/^\/(guide|games|privacy|404)(\/|$)/],
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/cdn\.jsdelivr\.net\//,
            handler: 'CacheFirst',
            options: {
              cacheName: 'cdn-fonts',
              expiration: { maxEntries: 30, maxAgeSeconds: 60 * 60 * 24 * 365 },
            },
          },
          {
            urlPattern: /\/fonts\//,
            handler: 'CacheFirst',
            options: {
              cacheName: 'local-fonts',
              expiration: { maxEntries: 20, maxAgeSeconds: 60 * 60 * 24 * 365 },
            },
          },
          {
            urlPattern: /\/logo\//,
            handler: 'CacheFirst',
            options: {
              cacheName: 'images',
              expiration: { maxEntries: 50, maxAgeSeconds: 60 * 60 * 24 * 30 },
            },
          },
          {
            urlPattern: ({ request }) => request.mode === 'navigate',
            handler: 'NetworkFirst',
            options: {
              cacheName: 'pages',
              expiration: { maxEntries: 50, maxAgeSeconds: 60 * 60 * 24 },
            },
          },
        ],
      }),
    ],
  });
