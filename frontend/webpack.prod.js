import { merge } from 'webpack-merge';
import { GenerateSW } from 'workbox-webpack-plugin';
import common from './webpack.common.js';

export default (env, argv) =>
  merge(common(env, { ...argv, mode: 'production' }), {
    devtool: 'source-map',
    plugins: [
      new GenerateSW({
        clientsClaim: true,
        navigateFallback: '/index.html',
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
