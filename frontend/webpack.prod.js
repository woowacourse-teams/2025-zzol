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
        // HTML 은 precache 하지 않는다. precache 한 index.html 을 내비게이션에 돌려주면
        // 재배포 뒤에도 옛 번들이 떠서 새 백엔드와 어긋난다(#1857). 내비게이션은 아래
        // NetworkFirst 가 받아 온라인이면 항상 새 HTML 을 쓰고, 오프라인일 때만 캐시로 떨어진다.
        exclude: [/\.map$/, /\.html$/],
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
