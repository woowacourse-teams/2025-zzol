import path from 'node:path';
/// <reference types="vitest/config" />
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    // 5173 을 고정한다. 구글 OAuth 승인된 자바스크립트 원본에 이 포트가 등록돼 있어
    // 포트가 밀리면 로그인이 origin_mismatch 로 막힌다.
    port: 5173,
    strictPort: true,
    // 개발 중에는 프록시로 같은 오리진처럼 만든다. 배포(nginx/CloudFront)와 같은 모양이라
    // 로컬에서만 되는 CORS 설정이 생기지 않는다.
    proxy: {
      '/admin/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
  },
});
