import { Tokens } from '../types';

export interface TokenStore {
  getAccessToken(): string | null;
  getRefreshToken(): string | null;
  setTokens(tokens: Tokens): void;
  clearTokens(): void;
}
