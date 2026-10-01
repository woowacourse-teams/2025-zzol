/// <reference types="vite/client" />

/**
 * 빌드 시점에 인라인되는 환경변수. 여기에 선언한 것만 타입이 잡힌다.
 * 오타로 다른 이름을 쓰면 undefined 가 조용히 들어가므로 목록을 명시한다.
 */
interface ImportMetaEnv {
  readonly VITE_GOOGLE_CLIENT_ID?: string;
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_ENV_NAME?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
