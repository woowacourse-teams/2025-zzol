import '@testing-library/jest-dom/vitest';

/**
 * jsdom 에는 스트림 API 가 다 있지는 않다. Node 18+ 의 전역 구현을 그대로 얹는다.
 * SSE 파서 테스트가 ReadableStream 과 TextEncoder 를 쓴다.
 */
import { ReadableStream } from 'node:stream/web';
import { TextDecoder, TextEncoder } from 'node:util';

if (!globalThis.ReadableStream) {
  globalThis.ReadableStream = ReadableStream as unknown as typeof globalThis.ReadableStream;
}
if (!globalThis.TextEncoder) {
  globalThis.TextEncoder = TextEncoder as unknown as typeof globalThis.TextEncoder;
}
if (!globalThis.TextDecoder) {
  globalThis.TextDecoder = TextDecoder as unknown as typeof globalThis.TextDecoder;
}
