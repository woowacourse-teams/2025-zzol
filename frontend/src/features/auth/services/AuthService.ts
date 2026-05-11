import { OAuthProvider } from '../types';

export interface AuthService {
  startOAuth(provider: OAuthProvider): void;
  handleCallback(params: URLSearchParams): Promise<void>;
  refresh(): Promise<void>;
  logout(): Promise<void>;
}
