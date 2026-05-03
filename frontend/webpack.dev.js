import { merge } from 'webpack-merge';
import common from './webpack.common.js';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.join(__dirname, '.env.development') });

const API_URL = process.env.API_URL || 'http://localhost:8080';

export default (env, argv) =>
  merge(common(env, { ...argv, mode: 'development' }), {
    devtool: 'inline-source-map',
    devServer: {
      proxy: [
        {
          // OAuth 플로우 프록시 — 같은 origin처럼 동작하게 만들어 CORS 우회
          context: ['/oauth2', '/login/oauth2'],
          target: API_URL,
          changeOrigin: true,
        },
      ],
    },
  });
