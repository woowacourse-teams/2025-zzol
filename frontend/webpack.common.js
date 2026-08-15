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
      new HtmlWebpackPlugin({
        template: './public/index.html',
        favicon: './public/favicon.ico',
        templateParameters: {
          DEV_SNIPPET:
            process.env.ENABLE_DEVTOOLS === 'true'
              ? `<script type="module" src="/devtools/devSnippet.js"></script>`
              : '',
        },
      }),
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
            from: 'public/sitemap.xml',
            to: 'sitemap.xml',
            transform(content) {
              const today = new Date().toISOString().split('T')[0];
              return content
                .toString()
                .replace(/<lastmod>[^<]*<\/lastmod>/g, `<lastmod>${today}</lastmod>`);
            },
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
      // 워크트리마다 다른 포트를 써야 여러 작업을 동시에 띄울 수 있다(#1660).
      // 고정이면 두 번째 워크트리가 EADDRINUSE 로 죽는다.
      port: Number(process.env.PORT) || 3000,
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
