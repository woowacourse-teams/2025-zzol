/**
 * 빌드 시점에 번들에 박히는 값들.
 *
 * <p>Vite 가 `VITE_*` 를 문자열로 인라인하므로 런타임 주입이 불가능하다. 그 제약이 여기서는
 * 안전장치가 된다. prod 번들에는 dev 백엔드 주소가 아예 들어가지 않아, dev 를 보고 있다고
 * 착각한 채 prod 에서 조치를 누르는 경로가 생기지 않는다.
 */

export type EnvName = 'LOCAL' | 'DEV' | 'PROD';

const RAW_ENV = import.meta.env.VITE_ENV_NAME;

export const ENV_NAME: EnvName =
  RAW_ENV === 'PROD' || RAW_ENV === 'DEV' ? RAW_ENV : 'LOCAL';

export const IS_PROD = ENV_NAME === 'PROD';

/** 비어 있으면 상대경로다. 로컬은 vite 프록시가, 배포는 앞단이 받는다. */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';
