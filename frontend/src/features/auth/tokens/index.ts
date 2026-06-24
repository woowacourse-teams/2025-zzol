export type { TokenStore } from './TokenStore';
export { CookieTokenStore } from './CookieTokenStore';
export { LocalStorageTokenStore } from './LocalStorageTokenStore';

// 백엔드 협의 완료 — JSON body 방식으로 확정, LocalStorageTokenStore 사용
import { LocalStorageTokenStore } from './LocalStorageTokenStore';

export const tokenStore = new LocalStorageTokenStore();
