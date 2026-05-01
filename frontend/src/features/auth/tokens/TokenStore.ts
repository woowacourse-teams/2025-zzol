import { Tokens } from '../types';

export interface TokenStore {
  getAccessToken(): string | null;
  setTokens(tokens: Tokens): void;
  clearTokens(): void;
}
