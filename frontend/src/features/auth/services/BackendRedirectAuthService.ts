import { authApi } from '../api/authApi';
import { TokenStore } from '../tokens/TokenStore';
import { OAuthProvider } from '../types';
import { AuthService } from './AuthService';

// Spring Security 표준 OAuth2 Client 방식
// 프론트 → /oauth2/authorization/{provider} redirect
//        → provider 인증 → 백엔드 /login/oauth2/code/{provider} 콜백
//        → OAuthSuccessHandler → 프론트 /auth/callback 으로 redirect (토큰 포함)
//
// ※ 프론트/백엔드 origin이 다를 때는 백엔드가 프론트 도메인으로 redirect해야 함.
//   백엔드 OAuthSuccessHandler에서 프론트 URL로 redirect 로직 필요.
export class BackendRedirectAuthService implements AuthService {
  constructor(private readonly tokenStore: TokenStore) {}

  startOAuth(provider: OAuthProvider): void {
    window.location.href = `${process.env.API_URL}/oauth2/authorization/${provider}`;
  }

  async handleCallback(params: URLSearchParams): Promise<void> {
    const accessToken = params.get('accessToken');
    const refreshToken = params.get('refreshToken');

    if (!accessToken || !refreshToken) {
      throw new Error('OAuth callback에 토큰이 없습니다');
    }

    this.tokenStore.setTokens({ accessToken, refreshToken });
  }

  async refresh(): Promise<void> {
    const refreshToken = this.tokenStore.getRefreshToken();
    if (!refreshToken) throw new Error('refresh token 없음');

    const tokens = await authApi.refresh(refreshToken);
    this.tokenStore.setTokens(tokens);
  }

  async logout(): Promise<void> {
    try {
      await authApi.logout();
    } finally {
      this.tokenStore.clearTokens();
    }
  }
}
