import { Tokens } from '../types';
import { TokenStore } from './TokenStore';

// Access token은 in-memory 보관, refresh token은 백엔드가 httpOnly cookie로 발급/관리
export class CookieTokenStore implements TokenStore {
  private accessToken: string | null = null;

  getAccessToken(): string | null {
    return this.accessToken;
  }

  getRefreshToken(): string | null {
    // httpOnly cookie는 JS에서 읽을 수 없음. 백엔드가 자동으로 cookie를 읽음.
    return null;
  }

  setTokens(tokens: Tokens): void {
    this.accessToken = tokens.accessToken;
  }

  clearTokens(): void {
    this.accessToken = null;
  }
}
