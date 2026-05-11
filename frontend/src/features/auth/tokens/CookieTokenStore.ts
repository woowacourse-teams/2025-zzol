import { Tokens } from '../types';
import { TokenStore } from './TokenStore';

// accessToken은 in-memory 보관, refreshToken은 HttpOnly 쿠키로 백엔드가 관리
export class CookieTokenStore implements TokenStore {
  private accessToken: string | null = null;

  getAccessToken(): string | null {
    return this.accessToken;
  }

  setTokens(tokens: Tokens): void {
    this.accessToken = tokens.accessToken;
  }

  clearTokens(): void {
    this.accessToken = null;
  }
}
